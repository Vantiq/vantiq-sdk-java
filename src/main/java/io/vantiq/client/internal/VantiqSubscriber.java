package io.vantiq.client.internal;

import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import okhttp3.*;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Internal class that manages subscriptions to a Vantiq server.
 */
public class VantiqSubscriber implements WebSocketListener {

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
            this.session.getServer().replace("http", "ws")
                + "/api/v" + this.session.getApiVersion()
                + "/wsock/websocket";
        Request request = new Request.Builder().url(url).build();

        WebSocketCall.create(client, request).enqueue(this);
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
        
        public VantiqSubscriptionRequest(String path, String accessToken) {
            new VantiqSubscriptionRequest(path, accessToken, null);
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

        try {
            VantiqSubscriptionRequest request =
                    new VantiqSubscriptionRequest(path, this.session.getAccessToken(), parameters);
            String body = VantiqSession.gson.toJson(request);
            this.webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, body));
        } catch(IOException ex) {
            callback.onFailure(ex);
        }
    }
    
    public void subscribe(String path, SubscriptionCallback callback) {
        subscribe(path, callback, null);
    }

    public void close() {
        if(this.scheduledExecutor != null) {
            this.scheduledExecutor.shutdown();
            this.scheduledExecutor = null;
        }
        if(this.webSocket != null) {
            try {
                this.webSocket.close(1000, null);
                this.webSocket = null;
            } catch(IOException ex) {
                this.lifecycleHandler.onFailure(ex);
            }
        }
    }

    //--------------------------------------------------------------------------
    // JSON Message Helper Classes
    //--------------------------------------------------------------------------

    private static class ValidateAuthenticationRequest {
        public String op = "validate";
        public String resourceName = "users";
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
                if(subscriber.webSocket != null) {
                    Buffer payload = new Buffer();
                    payload.writeString("Vantiq-Ping", StandardCharsets.ISO_8859_1);
                    subscriber.webSocket.sendPing(payload);
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
        try {
            //
            // Once the socket is open, we first need to validate the token
            // to create an authenticated session
            //
            ValidateAuthenticationRequest request =
                new ValidateAuthenticationRequest(this.session.getAccessToken());
            String body = VantiqSession.gson.toJson(request);
            this.webSocket.sendMessage(RequestBody.create(WebSocket.TEXT, body));

            //
            // Once connected, we start pinging if requested.  Pinging will continue
            // until the websocket is closed.
            //
            if(this.enablePings) {
                startPeriodicPings();
            }
        } catch(IOException ex) {
            this.lifecycleHandler.onFailure(ex);
        }
    }

    @Override
    public void onFailure(IOException e, Response response) {
        this.lifecycleHandler.onFailure(e);
    }

    @Override
    public void onMessage(ResponseBody body) throws IOException {
        try {
            String bodyStr;
            if(body.contentType() == WebSocket.TEXT) {
                bodyStr = body.string();
            } else {
                bodyStr = body.source().readByteString().utf8();
            }

            SubscriptionMessage msg =
                VantiqSession.gson.fromJson(bodyStr, SubscriptionMessage.class);

            String requestId = null;
            SubscriptionCallback callback = null;
            boolean isSubscribed = false;
            if(msg != null && msg.getHeaders() != null) {
                requestId = msg.getHeaders().get("X-Request-Id");
                callback = this.callbacks.get(requestId);
                isSubscribed = this.subscribed.get(requestId);
            }

            if (this.wsauthenticated) {

                // If the callback is null, this is the initial connection response
                if(callback == null) {
                    if(msg.getStatus() == 200) {
                        // No op as this is expected
                    } else {
                        this.lifecycleHandler.onError("Error authenticating WebSocket request", body);
                    }
                } else {

                    // If this is yet to be subscribed, then it's the subscription
                    // response
                    if(!isSubscribed) {
                        if(msg.getStatus() == 200) {
                            this.subscribed.put(requestId, Boolean.TRUE);
                            callback.onConnect();
                        } else if (msg.getStatus() == 100) {
g                            callback.onMessage(msg);
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
                    this.lifecycleHandler.onError("Error establishing authenticated WebSocket session", body);
                }
            }
        } finally {
            body.close();
        }
    }

    @Override
    public void onPong(Buffer payload) {
        // No-op for Pong frame
    }

    @Override
    public void onClose(int code, String reason) {
        this.lifecycleHandler.onClose();
    }
}
