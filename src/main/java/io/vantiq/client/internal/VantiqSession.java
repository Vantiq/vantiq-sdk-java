package io.vantiq.client.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.vantiq.client.ResponseHandler;
import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import okio.ByteString;

/**
 * Internal class that manages the authenticated access and interface
 * to the Vantiq REST API.
 */
public class VantiqSession {

    public final static MediaType APPLICATION_JSON = MediaType.parse("application/json");
    public final static MediaType PLAIN_TEXT = MediaType.parse("plain/text");
    public final static Gson                  gson = new Gson();

    public final static int DEFAULT_API_VERSION = 1;

    private OkHttpClient client = null;

    private String   server;
    private int      apiVersion;
    private Authenticator proxyAuthenticator = null;
    private Proxy proxy = null;
    private boolean  authenticated;
    private String   accessToken;
    private String   idToken;
    private String   username;

    private long readTimeout = 0;
    private long writeTimeout = 0;
    private long connectTimeout = 0;

    private VantiqSubscriber subscriber;

    public VantiqSession(String server) {
        this(server, DEFAULT_API_VERSION);
    }
    
    public VantiqSession(String server, Authenticator proxyAuthenticator) {
        this(server, DEFAULT_API_VERSION, proxyAuthenticator);
    }
    
    public VantiqSession(String server, int apiVersion) {
        this(server, apiVersion, null);
    }
    public VantiqSession(String server, int apiVersion, Authenticator proxyAuthenticator) {
        super();
        this.server     = server;
        this.apiVersion = apiVersion;
        this.proxyAuthenticator = proxyAuthenticator;
    
        createClient();
    }

    private void createClient() {
        
        //
        //  Unless we specifically ask for HTTP 1.1 we are at risk for this error:
        //
        //  ProtocolException: Expected ':status' header not present
        //
        //  See https://stackoverflow.com/questions/46807237/protocolexception-expected-status-header-not-present
        //  See https://stackoverflow.com/questions/49643383/http-2-protocol-not-working-with-okhttp
        //
        List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_1_1);
        
        boolean needProxyAuth = setupProxyAuthentication();
    
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .readTimeout(this.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(this.writeTimeout, TimeUnit.MILLISECONDS)
            .connectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS)
            .protocols(protocols);
        
        // If a proxy authenticator has been provided, set up our client to use it.
        if (needProxyAuth) {
            builder.proxySelector(ProxySelector.getDefault());
            // if a proxy authenticator has been provided, use it.  Otherwise, assume things will work out.
            if (this.proxyAuthenticator != null) {
                builder.proxyAuthenticator(this.proxyAuthenticator);
            }
        }
        this.client = builder.build();
    }
    
    private boolean setupProxyAuthentication() {
        if (proxyAuthenticator != null) {
            return true;
        } else {
            try {
                URI serverUri = new URI(this.server);
                // ask Java if we're using a proxy.
                List<Proxy> proxies = ProxySelector.getDefault().select(serverUri);
                if (proxies.size() == 0 || (proxies.size() == 1  && proxies.get(0).type() == Proxy.NO_PROXY.type())) {
                    return false;
                } else {
                    // Here, we'll need to define ourselves a simple authenticator
                    String scheme = serverUri.getScheme();
                    String proxyUser = System.getProperty(scheme.toLowerCase() + ".proxyUser");
                    String proxyPw = System.getProperty(scheme.toLowerCase() + ".proxyPassword");
                    if (proxyUser != null && proxyPw != null) {
                        this.proxyAuthenticator = (route, response) -> {
                            String credential = Credentials.basic(proxyUser, proxyPw);
                            return response.request().newBuilder()
                                           .header("Proxy-Authorization", credential)
                                           .build();
                        };
                    } else {
                        // If we can't find credentials, fall back to the default Java-provided one.
                        this.proxyAuthenticator = Authenticator.JAVA_NET_AUTHENTICATOR;
                    }
                    return true;
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
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
     *
     * @param accessToken The access token to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.authenticated = (this.accessToken != null? true: false);
    }

    /**
     * Returns the current idToken.  If not authenticated, this is null.
     *
     * @return The idToken or null if not an authenticated session or an old Vantiq server
     */
    public String getIdToken() {
        return this.idToken;
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
     * Returns the user associated with the connection
     *
     * @return The username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the user associated with the connection.
     *
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the server URL of the Vantiq system.
     *
     * @param server The server URL
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Returns the server URL of the Vantiq system.
     *
     * @return The server URL
     */
    public String getServer() {
        return this.server;
    }

    /**
     * Returns the API version used
     *
     * @return The API version
     */
    public int getApiVersion() {
        return this.apiVersion;
    }

    /**
     * Sets the read timeout for the connection
     *
     * @param timeout The timeout duration in milliseconds
     */
    public void setReadTimeout(long timeout) {
        this.readTimeout = timeout;
        createClient();
    }

    /**
     * Returns the read timeout for the connection
     *
     * @return The timeout duration in milliseconds
     */
    public long getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * Sets the write timeout for the connection
     *
     * @param timeout The timeout duration in milliseconds
     */
    public void setWriteTimeout(long timeout) {
        this.writeTimeout = timeout;
        createClient();
    }

    /**
     * Returns the write timeout for the connection
     *
     * @return The timeout duration in milliseconds
     */
    public long getWriteTimeout() {
        return this.writeTimeout;
    }

    /**
     * Sets the connect timeout for the connection
     *
     * @param timeout The timeout duration in milliseconds
     */
    public void setConnectTimeout(long timeout) {
        this.connectTimeout = timeout;
        createClient();
    }

    /**
     * Gets the connect timeout for the connection
     *
     * @return The timeout duration in milliseconds
     */
    public long getConnectTimeout() {
        return this.connectTimeout;
    }

    /**
     * This class provides a bridge between the {@link okhttp3.Callback} used
     * in the underlying OkHttp request and the {@link io.vantiq.client.ResponseHandler}
     */
    static class CallbackAdapter implements Callback {

        private ResponseHandler responseHandler;
        private boolean isStreamingResponse = false;

        protected void responseHook(Object body) {}

        public CallbackAdapter(ResponseHandler responseHandler) {
            this(responseHandler, /*isStreamingResponse=*/ false);
        }

        public CallbackAdapter(ResponseHandler responseHandler, boolean isStreamingResponse) {
            this.responseHandler = responseHandler;
            this.isStreamingResponse = isStreamingResponse;
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()) {
                Object body = VantiqResponse.extractBody(response, this.isStreamingResponse);
                responseHook(body);
                this.responseHandler.onSuccess(body, response);
            } else {
                try {
                    //
                    //  Normally when failing via an API call (such as "authenticate" or "select") the bodyString
                    //  will contain a JSON-string like this:
                    //
                    //  [{"code":"io.vantiq.authentication.failed","message":"Unauthorized","parms":[]}]
                    //  [{"code":"com.accessg2.ag2rs.type.not.found","message":"The type: XXX is not defined in the current namespace.","params":["XXX"]}]
                    //
                    //  These can be converted directly to VantiqError objects.
                    //
                    //  But sometimes we will get a bodyString like this:
                    //
                    //  {"error": "Authentication failed"}
                    //
                    //  In that case we just tease it apart as a JSON object and create a VantiqError manually.
                    //
                    String bodyString = response.body().string();

                    List<VantiqError> errors = null;
                    Type errorsType = new TypeToken<List<VantiqError>>(){}.getType();

                    try
                    {
                        errors = gson.fromJson(bodyString, errorsType);
                    }
                    catch (Exception ex)
                    {
                        errors = null;
                    }

                    if ((errors == null) && (bodyString.length() > 0))
                    {
                        JsonObject jo = null;
                        
                        try
                        {
                            jo = gson.fromJson(bodyString,JsonObject.class);
                        }
                        catch (Exception ex)
                        {
                            //System.err.println("Exception parsing '" + bodyString + "'");
                            //System.err.println(ex.toString());

                            String code = "io.vantiq.nonjson.error";
                            String message = bodyString + " (" + response.code() + ")";

                            VantiqError ve = new VantiqError(code,message,null);

                            errors = new ArrayList<VantiqError>();
                            errors.add(ve);
                        }
                       
                        if (jo != null)
                        {
                            errors = new ArrayList<VantiqError>();

                            String code = "io.vantiq.status";
                            String message = jo.get("error").getAsString() + " (" + response.code() + ")";

                            VantiqError ve = new VantiqError(code,message,null);

                            errors.add(ve);
                        }
                     }

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
     * Authenticates onto the Vantiq server with the provided credentials.  After
     * this call completes, the credentials are not stored.
     *
     * @param username        The username for the Vantiq server
     * @param password        The password for the Vantiq server
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */

    public VantiqResponse authenticate(final String username, String password, ResponseHandler responseHandler)
    {
        Callback cb = null;
        if (responseHandler != null)
        {
            cb = new CallbackAdapter(responseHandler)
            {
                @Override
                public void responseHook(Object body)
                {
                    JsonElement jsonBody = (JsonElement) body;
                    if (jsonBody != null && jsonBody.isJsonObject())
                    {
                        JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                        if (token != null)
                        {
                            VantiqSession.this.username = username;
                            VantiqSession.this.accessToken = token.getAsString();
                            VantiqSession.this.authenticated = true;

                            JsonElement idToken = ((JsonObject) jsonBody).get("idToken");

                            if (idToken != null)
                            {
                                VantiqSession.this.idToken = idToken.getAsString();
                            }
                        }
                    }
                }
            };
        }

        //
        //  The builtin Credentials.basic method from the okhttp library doesn't support anything but
        //  Latin username and password. Do it manually instead to support multibyte. (See #887)
        //
        String usernameAndPassword = username + ":" + password;
        String encoded = "Basic " + ByteString.encodeUtf8(usernameAndPassword).base64();

        VantiqResponse response = this.request(encoded, "GET", "authenticate",
                null, null, null, false, cb);

        if (response != null)
        {
            JsonElement jsonBody = (JsonElement) response.getBody();
            if (jsonBody != null && jsonBody.isJsonObject())
            {
                JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                if (token != null)
                {
                    this.username = username;
                    this.accessToken = token.getAsString();
                    this.authenticated = true;

                    JsonElement idToken = ((JsonObject) jsonBody).get("idToken");

                    if (idToken != null)
                    {
                        this.idToken = idToken.getAsString();
                    }

                }
            }
        }

        return response;
    }

    /**
     * Authenticates onto the Vantiq server with the provided credentials.  After
     * this call completes, the credentials are not stored.
     *
     * @param accessToken     The accessToken to be revoked
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */

    public VantiqResponse revoke(final String accessToken, ResponseHandler responseHandler)
    {
        Callback cb = null;

        if (responseHandler != null)
        {
            cb = new CallbackAdapter(responseHandler)
            {
                @Override
                public void responseHook(Object body)
                {

                    VantiqSession.this.username = null;
                    VantiqSession.this.accessToken = null;
                    VantiqSession.this.authenticated = false;
                }
            };
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.server).newBuilder();
        urlBuilder.addPathSegments("authenticate/revoke");


        // Build request
        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build());

        // Add body based on type
        RequestBody reqBody = null;

        builder.addHeader("Content-Type", PLAIN_TEXT.toString());
        builder.method("POST", RequestBody.create(PLAIN_TEXT, accessToken));


        // Finally construct the request
        Request request = builder.build();

        VantiqResponse response = null;

        // Execute the request either synchronously or asynchronously based on existence of callback
        if (cb != null)
        {
            client.newCall(request).enqueue(cb);
            return null;
        }
        else
        {
            try
            {
                response = VantiqResponse.createFromResponse(client.newCall(request).execute(), false);
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }


        if (response != null)
        {
            VantiqSession.this.username = null;
            VantiqSession.this.accessToken = null;
            VantiqSession.this.authenticated = false;
        }

        return response;
    }


    /**
     * Refreshes the supplied accessToken
     *
     * @param accessToken     The accessToken to be refreshed
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */

    public VantiqResponse refresh(final String accessToken, ResponseHandler responseHandler)
    {
        Callback cb = null;

        if (responseHandler != null)
        {
            cb = new CallbackAdapter(responseHandler)
            {
                @Override
                public void responseHook(Object body)
                {
                    JsonElement jsonBody = (JsonElement) body;
                    if (jsonBody != null && jsonBody.isJsonObject())
                    {
                        JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                        if (token != null)
                        {
                            VantiqSession.this.accessToken = token.getAsString();
                            VantiqSession.this.authenticated = true;

                            JsonElement idToken = ((JsonObject) jsonBody).get("idToken");

                            if (idToken != null)
                            {
                                VantiqSession.this.idToken = idToken.getAsString();
                            }
                        }
                    }
                }
            };
        }


        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.server).newBuilder();
        urlBuilder.addPathSegments("authenticate/refresh");


        // Build request
        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build());

        // Add body based on type
        RequestBody reqBody = null;

        builder.addHeader("Content-Type", PLAIN_TEXT.toString());
        builder.method("POST", RequestBody.create(PLAIN_TEXT, accessToken));


        // Finally construct the request
        Request request = builder.build();

        VantiqResponse response = null;

        // Execute the request either synchronously or asynchronously based on existence of callback
        if (cb != null)
        {
            client.newCall(request).enqueue(cb);
            return null;
        }
        else
        {
            try
            {
                response = VantiqResponse.createFromResponse(client.newCall(request).execute(), false);
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        
        if (response != null)
        {
            JsonElement jsonBody = (JsonElement) response.getBody();
            if (jsonBody != null && jsonBody.isJsonObject())
            {
                JsonElement token = ((JsonObject) jsonBody).get("accessToken");
                if (token != null)
                {
                    this.accessToken = token.getAsString();
                    this.authenticated = true;

                    JsonElement idToken = ((JsonObject) jsonBody).get("idToken");

                    if (idToken != null)
                    {
                        this.idToken = idToken.getAsString();
                    }
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
     * Perform a HTTP GET request against a given raw path
     *
     * @param fullPath The unencoded partial path for the GET (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param extraHeaders   optional headers   
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */
    public VantiqResponse rawGet(String fullPath,
                              Map<String,String> queryParams,
                              Map<String,String> extraHeaders,
                              ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "GET", fullPath, queryParams, extraHeaders, null, false, cb);
    }

    /**
     * Perform a HTTP GET request against a given path
     *
     * @param path The unencoded partial path for the GET (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */
    public VantiqResponse get(String path,
                              Map<String,String> queryParams,
                              ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "GET", fullpath(path), queryParams, null, null, false, cb);
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
     * @return The response from the Vantiq server
     */
    public VantiqResponse post(String path,
                               Map<String,String> queryParams,
                               String body,
                               ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "POST", fullpath(path), queryParams, null, body, false, cb);
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
     * @return The response from the Vantiq server
     */
    public VantiqResponse put(String path,
                              Map<String,String> queryParams,
                              String body,
                              ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "PUT", fullpath(path), queryParams, null, body, false, cb);
    }

    /**
     * Perform a HTTP DELETE request against a given path
     *
     * @param path The unencoded partial path for the DELETE (without any query parameters)
     * @param queryParams The unencoded query parameters included in the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */
    public VantiqResponse delete(String path,
                                 Map<String,String> queryParams,
                                 ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);
        return this.request(authValue(), "DELETE", fullpath(path), queryParams, null, null, false, cb);
    }

    /**
     * Uploads the given file.
     *
     * @param path The path for the resource to upload into (usually "/resources/documents", "/resources/images" or "resources/videos")
     * @param file The file to upload
     * @param contentType The MIME type of the file uploaded
     * @param documentPath The path of the file in the Vantiq system
     * @param queryParams The unencoded query parameters included in the request
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */
    public VantiqResponse upload(String path,
                                 File file,
                                 String contentType,
                                 String documentPath,
                                 Map<String,String> queryParams,
                                 ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler) : null);

        // Build the multi-part request body
        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), file);
        RequestBody reqBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("defaultName", documentPath, fileBody)
            .build();
        return this.request(authValue(), "POST", fullpath(path), queryParams, null, reqBody, false, cb);
    }

    /**
     * Downloads the given file.  The response body will be a stream that can be used to
     * download the content of the file.
     *
     * @param path The path of the file to download
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     * @return The response from the Vantiq server
     */
    public VantiqResponse download(String path, ResponseHandler responseHandler) {
        Callback cb = (responseHandler != null ? new CallbackAdapter(responseHandler, true) : null);
        return this.request(authValue(), "GET", path, null, null, null, true, cb);
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
     * @param body The optional request body to include.  This can be a String or
     *             pre-built RequestBody
     * @param isStreamingResponse If true, the response should not download the entire response,
     *                            rather a BufferedSource should be returned to allow the
     *                            client to pull the data.  This is useful for downloading large content.
     * @param callback The callback that is called to handle the HTTP response.  If not null,
     *                 this executes asynchronously.  If null, then this executes synchronously
     *                 and returns the response.
     */
    private VantiqResponse request(String authValue,
                                   String method,
                                   String path,
                                   Map<String,String> queryParams,
                                   Map<String,String> extraHeaders,
                                   Object body,
                                   boolean isStreamingResponse,
                                   Callback callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.server).newBuilder();
        urlBuilder.addPathSegments(path);
        if(queryParams != null) {
            for(Map.Entry<String,String> param : queryParams.entrySet()) {
                urlBuilder.setQueryParameter(param.getKey(), param.getValue());
            }
        }

        // Build request
        Request.Builder builder = new Request.Builder()
            .url(urlBuilder.build())
            .addHeader("Authorization", authValue);

        if (extraHeaders != null)
        {
            for (Map.Entry<String, String> item : extraHeaders.entrySet()) 
            {
                String key = item.getKey();
                String value = item.getValue();
                
                builder.addHeader(key,value);
            }
        }
        
        // Add body based on type
        RequestBody reqBody = null;
        if(body != null) {
            if(body instanceof String) {
                builder.addHeader("Content-Type", APPLICATION_JSON.toString());
                builder.method(method, RequestBody.create(APPLICATION_JSON, (String) body));
            } else if(body instanceof RequestBody) {
                builder.method(method, (RequestBody) body);
            } else {
                throw new IllegalArgumentException("Illegal request body type.  Must be 'String' or 'RequestBody'");
            }
        } else {
            builder.method(method, null);
        }

        // Finally construct the request
        Request request = builder.build();

        // Execute the request either synchronously or asynchronously based on existence of callback
        if(callback != null) {
            client.newCall(request).enqueue(callback);
            return null;
        } else {
            try {
                return VantiqResponse.createFromResponse(client.newCall(request).execute(), isStreamingResponse);
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
     * @param enablePings Indicates if pings should be enabled to ensure the websocket stays open
     * @param parameters Parameters
     */
    public void subscribe(final String path,
                          final SubscriptionCallback callback,
                          boolean enablePings,
                          final Map<String, Object> parameters) {
        if(!this.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }

        if(this.subscriber == null) {
            this.subscriber = new VantiqSubscriber(this, client, enablePings);
            this.subscriber.connect(new VantiqSubscriberLifecycleListener() {
                @Override
                public void onConnect() {
                    VantiqSession.this.subscriber.subscribe(path, callback, parameters);
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
            this.subscriber.subscribe(path, callback, parameters);
        }
    }


    /**
     * Acknowledge the receipt of a reliable message
     *
     * @param requestId Request Id
     * @param subscriptionId Subscription Id
     * @param sequenceId Sequence Id
     * @param partitionId Partition Id
     * 
     * @throws IOException If request fails    
     */
    public void ack(String requestId, String subscriptionId, Double sequenceId, Double partitionId) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("subscriptionId", subscriptionId);
        params.put("sequenceId", sequenceId);
        params.put("partitionId", partitionId);
        this.subscriber.ack(requestId, subscriptionId, sequenceId, partitionId);
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

    /**
     * Closes the WebSocket to the Vantiq server.
     */
    public void close() {
        this.subscriber.close();
        this.subscriber = null;

    }
}