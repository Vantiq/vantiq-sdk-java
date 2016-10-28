package io.vantiq.client.internal;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vantiq.client.*;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * Internal class that manages the authenticated access and interface
 * to the Vantiq REST API.
 */
public class VantiqSession {

    public final static MediaType APPLICATION_JSON = MediaType.parse("application/json");
    public final static JsonParser          parser = new JsonParser();
    public final static Gson                  gson = new Gson();

    public final static int DEFAULT_API_VERSION = 1;

    private final OkHttpClient client = new OkHttpClient();

    private String   server;
    private int      apiVersion;
    private boolean  authenticated;
    private String   accessToken;

    private VantiqSubscriber subscriber;

    public VantiqSession(String server) {
        this(server, DEFAULT_API_VERSION);
    }

    public VantiqSession(String server, int apiVersion) {
        super();
        this.server     = server;
        this.apiVersion = apiVersion;
    }

    /**
     * Returns if the current session is authenticated.
     *
     * @return true if authenticated successfully
     */
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    /**
     * Sets the access token for use in future requests to the Vantiq server.  This
     * can be a normal or long-lived token.
     *
     * Since the access token is explicitly set, we assume the session is
     * authenticated if the access token is not null.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        if(this.accessToken != null) {
            this.authenticated = true;
        }
    }

    /**
     * Returns the current access token.  If not authenticated, this is null.
     *
     * @return The access token used for requests or null if not an authenticated session.
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Returns the server URL of the Vantiq system.
     */
    public String getServer() {
        return this.server;
    }

    /**
     * Returns the API version used
     */
    public int getApiVersion() {
        return this.apiVersion;
    }

    /**
     * This class provides a bridge between the {@link okhttp3.Callback} used
     * in the underlying OkHttp request and the {@link io.vantiq.client.ResponseHandler}
     */
    static class CallbackAdapter implements Callback {

        private ResponseHandler responseHandler;

        protected void responseHook(Object body) {}

        public CallbackAdapter(ResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()) {
                Object body = VantiqResponse.extractBody(response);
                responseHook(body);
                this.responseHandler.onSuccess(body, response);
            } else {
                this.responseHandler.onError(VantiqResponse.extractErrors(response), response);
            }
        }

        @Override
        public void onFailure(Call call, IOException t) {
            this.responseHandler.onFailure(t);
        }

    }

    /**
     * Perform Base64 encoding.  Since Android has some limitations, we detect what's available
     * and use that.
     */
    private String encode(String value) {
        return BaseEncoding.base64().encode(value.getBytes());
    }

    /**
     * Authenticates onto the Vantiq server with the provided credentials.  After
     * this call completes, the credentials are not stored.
     *
     * @param username The username for the Vantiq server
     * @param password The password for the Vantiq server
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public VantiqResponse authenticate(String username, String password, ResponseHandler responseHandler) {
        String authValue = "Basic " + encode(username + ":" + password);

        Callback cb = null;
        if(responseHandler != null) {
            cb = new CallbackAdapter(responseHandler) {
                @Override
                public void responseHook(Object body) {
                    JsonElement jsonBody = (JsonElement) body;
                    if (jsonBody != null && jsonBody.isJsonObject()) {
                        JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                        if (token != null) {
                            VantiqSession.this.accessToken = token.getAsString();
                            VantiqSession.this.authenticated = true;
                        }
                    }
                }
            };
        }

        VantiqResponse response = this.request(authValue, "GET", "authenticate", null, null, cb);
        if(response != null) {
            JsonElement jsonBody = (JsonElement) response.getBody();
            if (jsonBody != null && jsonBody.isJsonObject()) {
                JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                if (token != null) {
                    this.accessToken = token.getAsString();
                    this.authenticated = true;
                }
            }
        }

        return response;
    }

    /**
     * Check that the session has been authenticated.  If not authenticated
     * this throws an IllegalStateException.  Otherwise, this is a no-op.
     */
    private String authValue() throws IllegalStateException {
        if(!isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        return "Bearer " + this.accessToken;
    }

    /**
     * Returns the full path for the HTTP request given the path
     *
     * @param path The partial path (everything past <code>/api/v#/</code>
     */
    private String fullpath(String path) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        return "api/v" + this.apiVersion + "/" + path;
    }

    /**
     * Perform a HTTP GET request against a given path
     *
     * @param path The unencoded partial path for the GET (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public VantiqResponse get(String path,
                              Map<String,String> queryParams,
                              ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "GET", fullpath(path), queryParams, null, cb);
    }

    /**
     * Perform a HTTP POST request against a specific path
     *
     * @param path The unencoded partial path for the POST (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param body The JSON encoding string included in the body of the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public VantiqResponse post(String path,
                               Map<String,String> queryParams,
                               String body,
                               ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "POST", fullpath(path), queryParams, body, cb);
    }

    /**
     * Perform a HTTP PUT request for a specific path
     *
     * @param path The unencoded partial path for the PUT (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param body The JSON encoding string included in the body of the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public VantiqResponse put(String path,
                              Map<String,String> queryParams,
                              String body,
                              ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "PUT", fullpath(path), queryParams, body, cb);
    }

    /**
     * Perform a HTTP DELETE request against a given path
     *
     * @param path The unencoded partial path for the DELETE (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public VantiqResponse delete(String path,
                                 Map<String,String> queryParams,
                                 ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "DELETE", fullpath(path), queryParams, null, cb);
    }

    //----------------------------------------------------------------
    // Request support using OkHttp
    //----------------------------------------------------------------

    /**
     * All requests to the Vantiq server are performed using this
     * method.  All request bodies are expected to be JSON.
     *
     * @param authValue The value of the "Authorization" header.
     * @param method The HTTP method to use (e.g. GET, POST, etc)
     * @param path The full unencoded URL path to use (without query parameters)
     * @param queryParams The unencoded query parameters to include in the request
     * @param body The optional request body to include.
     * @param callback The callback that is called to handle the HTTP response.  If not null,
     *                 this executes asynchronously.  If null, then this executes synchronously
     *                 and returns the response.
     */
    private VantiqResponse request(String authValue,
                                   String method,
                                   String path,
                                   Map<String,String> queryParams,
                                   String body,
                                   Callback callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.server).newBuilder();
        urlBuilder.addPathSegments(path);
        if(queryParams != null) {
            for(Map.Entry<String,String> param : queryParams.entrySet()) {
                urlBuilder.setQueryParameter(param.getKey(), param.getValue());
            }
        }

        RequestBody reqBody = null;
        if(body != null) {
            reqBody = RequestBody.create(APPLICATION_JSON, body);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Content-Type", APPLICATION_JSON.toString())
                .addHeader("Authorization", authValue)
                .method(method, reqBody)
                .build();

        if(callback != null) {
            client.newCall(request).enqueue(callback);
            return null;
        } else {
            try {
                return VantiqResponse.createFromResponse(client.newCall(request).execute());
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    //----------------------------------------------------------------
    // Subscription Support using OkHttp
    //----------------------------------------------------------------

    /**
     * Subscribes to a specific Vantiq event based on the given path.  A
     * websocket connection is used to listen for the events.  If no connection
     * is already established, then a connection is initiated.
     *
     * @param path The path that defines the event (e.g. /resource/id[/operation])
     * @param callback The callback that is executed for every event that occurs.
     */
    public void subscribe(final String path, final SubscriptionCallback callback) {
        if(!this.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }

        if(this.subscriber == null) {
            this.subscriber = new VantiqSubscriber(this, client);
            this.subscriber.connect(new VantiqSubscriberLifecycleListener() {
                @Override
                public void onConnect() {
                    VantiqSession.this.subscriber.subscribe(path, callback);
                }

                @Override
                public void onError(String message, ResponseBody body) {
                    callback.onError(message);
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }

                @Override
                public void onClose() {
                    // No-op
                }
            });
        } else {
            this.subscriber.subscribe(path, callback);
        }
    }

    /**
     * Unsubscribes to all current subscriptions by closing the WebSocket to the Vantiq
     * server.
     */
    public void unsubscribeAll() {
        if(this.subscriber != null) {
            this.subscriber.close();
            this.subscriber = null;
        }
    }
}