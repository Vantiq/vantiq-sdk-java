package io.vantiq.client.internal;

import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import okhttp3.*;
import okio.Buffer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Internal class that manages subscriptions to a Vantiq server.
 */
public class VantiqSubscriber extends WebSocketListener {

    private VantiqSession                              session = null;
    private OkHttpClient                                client = null;
    private WebSocket                                webSocket = null;
    private VantiqSubscriberLifecycleListener lifecycleHandler = null;
    private boolean                                enablePings = false;
    private ScheduledExecutorService         scheduledExecutor = null;
    private ScheduledFuture                       pingerHandle = null;

    private boolean                            wsauthenticated = false;

    private Map<String,SubscriptionCallback>         callbacks = new HashMap<String,SubscriptionCallback>();
    private Map<String,Boolean>                     subscribed = new HashMap<String,Boolean>();

    public VantiqSubscriber(VantiqSession session, OkHttpClient client, boolean enablePings) {
        this.session     = session;
        this.client      = client;
        this.enablePings = enablePings;

        if(this.enablePings) {
            this.scheduledExecutor = Executors.newScheduledThreadPool(1);
        }
    }

    public void connect(VantiqSubscriberLifecycleListener lifecycleHandler) {
        this.lifecycleHandler = lifecycleHandler;

        if (!this.session.isAuthenticated()) {
            throw new IllegalStateException("Session must be authenticated to subscribe to Vantiq events");
        }

        String url =
            this.session.getServer().replace("http", "ws");
        // Need to normalize trailing slash in server URI.
        if (!(url.endsWith("/"))) {
            url = url + "/";
        }
        
        url = url
                + "api/v" + this.session.getApiVersion()
                + "/wsock/websocket";
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, this);
    }

    private static class VantiqSubscriptionRequest {
        public String op = "subscribe";
        public String resourceName = "events";
        public String resourceId;
        public String accessToken;
        public Map<String,Object> parameters = new HashMap<String,Object>();

        public VantiqSubscriptionRequest(String path, String accessToken, Map<String, Object> parameters) {
            this.resourceId = path;
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            this.parameters.put("requestId", path);
            this.accessToken = accessToken;
        }
    }

    private static class VantiqAcknowledgementRequest {
        public String op = "acknowledge";
        public String resourceName = "events";
        public String resourceId;
        public String accessToken;
        public Map<String,Object> parameters = new HashMap<String,Object>();

        public VantiqAcknowledgementRequest(String path, String accessToken, Map<String, Object> parameters) {
            this.resourceId = path;
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            this.parameters.put("requestId", path);

            this.accessToken = accessToken;
        }
    } 
    
    public void subscribe(String path, SubscriptionCallback callback, Map<String, Object> parameters) {
        if (!this.wsauthenticated) {
            throw new IllegalStateException("Must be connected to subscribe to events");
        }

        if (this.callbacks.containsKey(path)) {
            throw new IllegalStateException("Callback already registered for event: " + path);
        } else {
            this.callbacks.put(path, callback);
            this.subscribed.put(path, Boolean.FALSE);
        }

        VantiqSubscriptionRequest request =
                new VantiqSubscriptionRequest(path, this.session.getAccessToken(), parameters);
        String body = VantiqSession.gson.toJson(request);
        this.webSocket.send(body);
    }

    public void ack(String requestId, String subscriptionId, Double sequenceId, Double partitionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("subscriptionId", subscriptionId);
        params.put("sequenceId", sequenceId);
        params.put("partitionId", partitionId);
        VantiqAcknowledgementRequest request =
                new VantiqAcknowledgementRequest(requestId, this.session.getAccessToken(), params);
        String body = VantiqSession.gson.toJson(request);
        this.webSocket.send(body);
    }

    public void close() {
        if (this.scheduledExecutor != null) {
            this.scheduledExecutor.shutdown();
            this.scheduledExecutor = null;
        }
        if (this.webSocket != null) {
            this.webSocket.close(1000, null);
            this.webSocket = null;
        }
    }

    //--------------------------------------------------------------------------
    // JSON Message Helper Classes
    //--------------------------------------------------------------------------

    private static class ValidateAuthenticationRequest {
        public String op = "validate";
        public String resourceName = "system.credentials";
        public String object;

        public ValidateAuthenticationRequest(String accessToken) {
            this.object = accessToken;
        }
    }

    //--------------------------------------------------------------------------
    // WebSocketListener API
    //--------------------------------------------------------------------------

    private class WebSockerPinger implements Runnable {

        @Override
        public void run() {
            VantiqSubscriber subscriber = VantiqSubscriber.this;
            try {
                if (subscriber.webSocket != null) {
                    Buffer payload = new Buffer();
                    payload.writeString("Vantiq-Ping", StandardCharsets.ISO_8859_1);
                    subscriber.webSocket.send(payload.readByteString());
                } else {
                    subscriber.pingerHandle.cancel(true);
                    subscriber.pingerHandle = null;
                }
            } catch(Exception ex) {
                subscriber.lifecycleHandler.onFailure(ex);
            }
        }

    }

    public void startPeriodicPings() {
        this.pingerHandle =
            this.scheduledExecutor.scheduleAtFixedRate(new WebSockerPinger(),
                                                       0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        this.webSocket = webSocket;
        //
        // Once the socket is open, we first need to validate the token
        // to create an authenticated session
        //
        ValidateAuthenticationRequest request =
            new ValidateAuthenticationRequest(this.session.getAccessToken());
        String body = VantiqSession.gson.toJson(request);
        this.webSocket.send(body);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
        this.lifecycleHandler.onFailure(t);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, ByteString bodyBytes) {
        String bodyStr = bodyBytes.utf8();
        SubscriptionMessage msg =
            VantiqSession.gson.fromJson(bodyStr, SubscriptionMessage.class);

        String requestId = null;
        SubscriptionCallback callback = null;
        boolean isSubscribed = false;
        if (msg != null && msg.getHeaders() != null) {
            requestId = msg.getHeaders().get("X-Request-Id");
            callback = this.callbacks.get(requestId);
            isSubscribed = this.subscribed.get(requestId);
        }

        if (this.wsauthenticated) {

            // If the callback is null, this is the initial connection response
            if (callback == null) {
                if (msg.getStatus() == 200) {
                    //
                    // Once connected, we start pinging if requested.  Pinging will continue
                    // until the websocket is closed.
                    //
                    // (Moved from onOpen so that the pings don't interfere with the authentication- this was causing
                    // tests to fail)
                    //
                    if (this.enablePings) {
                        startPeriodicPings();
                    }
                } else {
                    this.lifecycleHandler.onError("Error authenticating WebSocket request", null);
                }
            } else {

                // If this is yet to be subscribed, then it's the subscription
                // response
                if(!isSubscribed) {
                    if(msg.getStatus() == 200) {
                        this.subscribed.put(requestId, Boolean.TRUE);
                        callback.onConnect();
                    } else if (msg.getStatus() == 100) {
                        callback.onMessage(msg);
                    } else {
                        callback.onError("Error subscribing to '" + requestId + "'");
                    }
                } else {
                    callback.onMessage(msg);
                }

            }

        } else {
            if (msg.getStatus() == 200) {
                this.wsauthenticated = true;
                this.lifecycleHandler.onConnect();
            } else {
                this.lifecycleHandler.onError("Error establishing authenticated WebSocket session", null);
            }
        }
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        this.lifecycleHandler.onClose();
    }
}
