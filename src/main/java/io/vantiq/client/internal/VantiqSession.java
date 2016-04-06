package io.vantiq.client.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.vantiq.client.ResponseHandler;
import io.vantiq.client.VantiqError;
import okhttp3.*;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class VantiqSession {

    public final static int DEFAULT_API_VERSION = 1;

    private String   server;
    private int      apiVersion;
    private boolean  authenticated;
    private String   accessToken;

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
     */
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    /**
     * Returns the current access token.  If not authenticated, this is null.
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    static class CallbackAdapter implements Callback {

        private static final JsonParser parser = new JsonParser();

        private static final Gson gson = new Gson();

        private ResponseHandler responseHandler;

        public CallbackAdapter(ResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
        }

        protected void responseHook(JsonElement jsonBody) {}

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()) {

                // Parse response body
                String body = response.body().string();
                if("application/json".equals(response.header("Content-Type"))) {
                    if (body != null && body.length() > 0) {
                        JsonElement jsonBody = parser.parse(body);
                        responseHook(jsonBody);
                        this.responseHandler.onSuccess(jsonBody, response);
                    } else {
                        this.responseHandler.onSuccess(null, response);
                    }
                } else {
                    // If not JSON, just return the string value
                    this.responseHandler.onSuccess(body, response);
                }
            } else {
                try {
                    Type errorsType = new TypeToken<List<VantiqError>>(){}.getType();
                    List<VantiqError> errors = gson.fromJson(response.body().string(), errorsType);
                    this.responseHandler.onError(errors, response);
                } catch(IOException ex) {
                    this.responseHandler.onFailure(ex);
                }
            }
        }

        @Override
        public void onFailure(Call call, IOException t) {
            this.responseHandler.onFailure(t);
        }

    }


    /**
     * Authenticates onto the Vantiq server using the
     * provided credentials.
     *
     * @param username String
     * @param password String
     */
    public void authenticate(String username, String password, ResponseHandler responseHandler) {
        String authValue = "Basic " +
                DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());

        this.request(authValue, "GET", "authenticate", null, null, new CallbackAdapter(responseHandler) {
            @Override
            public void responseHook(JsonElement jsonBody) {
                if(jsonBody.isJsonObject()) {
                    JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                    if(token != null) {
                        VantiqSession.this.accessToken = token.getAsString();
                        VantiqSession.this.authenticated = true;
                    }
                }
            }
        });
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
     */
    private String fullpath(String path) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        return "api/v" + this.apiVersion + "/" + path;
    }

    /**
     * Perform a HTTP GET request against a given path
     */
    public void get(String path,
                    Map<String,String> queryParams,
                    ResponseHandler responseHandler) {
        this.request(authValue(), "GET", fullpath(path),
                     queryParams, null, new CallbackAdapter(responseHandler));
    }

    /**
     * Perform a HTTP POST request against a specific path
     */
    public void post(String path,
                     Map<String,String> queryParams,
                     String body,
                     ResponseHandler responseHandler) {
        this.request(authValue(), "POST", fullpath(path),
                     queryParams, body, new CallbackAdapter(responseHandler));
    }

    /**
     * Perform a HTTP PUT request for a specific path
     */
    public void put(String path,
                    Map<String,String> queryParams,
                    String body,
                    ResponseHandler responseHandler) {
        this.request(authValue(), "PUT", fullpath(path),
                     queryParams, body, new CallbackAdapter(responseHandler));
    }

    /**
     * Perform a HTTP DELETE request against a given path
     */
    public void delete(String path,
                       Map<String,String> queryParams,
                       ResponseHandler responseHandler) {
        this.request(authValue(), "DELETE", fullpath(path),
                     queryParams, null, new CallbackAdapter(responseHandler));
    }

    //----------------------------------------------------------------
    // Request support using OkHttp
    //----------------------------------------------------------------

    private void request(String authValue,
                         String method,
                         String path,
                         Map<String,String> queryParams,
                         String body,
                         Callback callback) {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.server).newBuilder();
        urlBuilder.addPathSegments(path);
        if(queryParams != null) {
            for(Map.Entry<String,String> param : queryParams.entrySet()) {
                urlBuilder.setQueryParameter(param.getKey(), param.getValue());
            }
        }

        RequestBody reqBody = null;
        if(body != null) {
            reqBody = RequestBody.create(MediaType.parse("application/json"), body);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authValue)
                .method(method, reqBody)
                .build();

        client.newCall(request).enqueue(callback);
    }
}