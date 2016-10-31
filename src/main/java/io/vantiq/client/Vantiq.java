package io.vantiq.client;

import com.google.gson.*;
import io.vantiq.client.internal.VantiqSession;
import okhttp3.Response;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Vantiq SDK for Java/Android API
 */
public class Vantiq {

    public enum SystemResources {
        ANALYTICS_MODELS("analyticsmodels"),
        DOCUMENTS("documents"),
        CONFIGURATIONS("configurations"),
        NAMESPACES("namespaces"),
        NODES("nodes"),
        PROCEDURES("procedures"),
        PROFILES("profiles"),
        RULES("rules"),
        SCALARS("scalars"),
        SOURCES("sources"),
        TOPICS("topics"),
        TYPES("types"),
        USERS("users");

        private String value;

        private SystemResources(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    private static Gson gson = new Gson();

    public enum TypeOperation {
        INSERT, UPDATE, DELETE
    }

    private VantiqSession session;

    /**
     * Response handler that simply delegates to the given response handler.
     * This is provided, so that the Vantiq class can override only what is
     * required.
     */
    class PassThruResponseHandler implements ResponseHandler {

        protected ResponseHandler delegate;

        public PassThruResponseHandler(ResponseHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSuccess(Object body, Response response) {
            this.delegate.onSuccess(body, response);
        }

        @Override
        public void onError(List<VantiqError> errors, Response response) {
            this.delegate.onError(errors, response);
        }

        @Override
        public void onFailure(Throwable t) {
            this.delegate.onFailure(t);
        }
    }

    /**
     * Constructs a Vantiq SDK instance against a specific Vantiq server
     * URL using a specific REST API version.
     *
     * @param server     The URL of the Vantiq server
     * @param apiVersion The REST API version to use
     */
    public Vantiq(String server, int apiVersion) {
        this.session = new VantiqSession(server, apiVersion);
    }

    /**
     * Constructs a Vantiq SDK instance against a specific Vantiq server
     * using the latest version of the API.
     *
     * @param server     The URL of the Vantiq server
     */
    public Vantiq(String server) {
        this.session = new VantiqSession(server);
    }

    /**
     * Returns if this Vantiq instance has been successfully
     * authenticated with the Vantiq server instance.
     *
     * @return true if authenticated successfully
     */
    public boolean isAuthenticated() {
        return this.session.isAuthenticated();
    }

    /**
     * Returns the server that was assigned when this user was
     * successfully logged in with the Vantiq server instance (or null
     * if not currently logged in).
     *
     * @return server string or null
     */
    public String getServer() {
        return this.session.getServer();
    }

    /**
     *  Sets the server; this is used if the app has remembered an old server and wants to re-use it. (It
     *  could also be used to "un-authenticate", setting the server to null.
     *
     *  @param server The server to be used to avoid calling "authenticate"
     */
    public void setServer(String server) {
        this.session.setServer(server);
    }

    /**
     * Returns the username that was assigned when this user was
     * successfully logged in with the Vantiq username instance (or null
     * if not currently logged in).
     *
     * @return username string or null
     */
    public String getUsername() {
        return this.session.getUsername();
    }

    /**
     *  Sets the username; this is used if the app has remembered an old username and wants to re-use it. (It
     *  could also be used to "un-authenticate", setting the username to null.
     *
     *  @param username The username to be used to avoid calling "authenticate"
     */
    public void setUsername(String username) {
        this.session.setUsername(username);
    }


    /**
     * Authenticates this Vantiq instance using the given credentials asynchronously.  The response
     * handler "onSuccess" will return true if the authentication was successful.
     *
     * @param username The username for the Vantiq server
     * @param password The password for the Vantiq server
     * @param responseHandler The response handler that is called upon completion.
     */
    public void authenticate(String username, String password, ResponseHandler responseHandler) {
        this.session.authenticate(username, password, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                // Simply return true as it was successful
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Authenticates this Vantiq instance using the given credentials synchronously.
     *
     * @param username The username for the Vantiq server
     * @param password The password for the Vantiq server
     * @return The response from the Vantiq server
     */
    public VantiqResponse authenticate(String username, String password) {
        VantiqResponse response = this.session.authenticate(username, password, null);
        if(response != null) {
            response.setBody(response.isSuccess());
        }
        return response;
    }

    /**
     * Performs a query to search for records that match the given constraints asynchronously.
     * The response body will be a List of JsonObject objects.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param propSpecs The optional list of properties to return in each record.  A null or empty list returns all properties.
     * @param where The optional where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @param sortSpec The optional sort specification to order the returned records.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void select(String resource,
                       List<String> propSpecs,
                       Object where,
                       SortSpec sortSpec,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        if(propSpecs != null) {
            queryParams.put("props", VantiqSession.gson.toJson(propSpecs));
        }
        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }
        if(sortSpec != null) {
            queryParams.put("sort", VantiqSession.gson.toJson(sortSpec.serialize()));
        }
        this.session.get(path, queryParams, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                if(body instanceof JsonArray) {
                    JsonArray arr = (JsonArray) body;
                    List<JsonObject> resultBody = new ArrayList<JsonObject>();
                    for(int i=0; i<arr.size(); i++) {
                        resultBody.add((JsonObject) arr.get(i));
                    }
                    this.delegate.onSuccess(resultBody, response);
                }
            }
        });
    }

    /**
     * Performs a batch query, specified by a JSON array.
     *
     * This is currently for internal use only and is not documented.
     *
     * @param requests A JsonArray of "request objects", each looking something like this:
     *                 {
     *                      "method": "GET",
     *                      "headers": {
     *                          "Content-type": "application/json"
     *                      },
     *                      "uri": /types?where={"name":"ArsType"}
     *                 }
     *
     *                 Note that anything past the "?" in the "uri" (which means the "where" clause in this case)
     *                 must be URL encoded.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void batch(JsonArray requests,
                       ResponseHandler responseHandler) {
        String path = "/batch";

        Map<String,String> queryParams = new HashMap<String,String>();

        for (int i=0; i<requests.size(); i++)
        {
            JsonObject jo = (JsonObject)requests.get(i);
            JsonObject headers = (JsonObject)jo.get("headers");

            headers.addProperty("Authorization","Bearer " + this.getAccessToken());
        }

        String body = requests.toString();

        this.session.post(path, queryParams, body, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                if(body instanceof JsonArray) {
                    JsonArray arr = (JsonArray) body;
                    List<JsonObject> resultBody = new ArrayList<JsonObject>();
                    for(int i=0; i<arr.size(); i++) {
                        resultBody.add((JsonObject) arr.get(i));
                    }
                    this.delegate.onSuccess(resultBody, response);
                }
            }
        });
    }

    /**
     * Returns the record for the given resource and specified id.  The response is a single JsonObject.
     * Performs a query to search for records that match the given constraints synchronously.
     * The response body will be a List of JsonObject objects.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param propSpecs The optional list of properties to return in each record.  A null or empty list returns all properties.
     * @param where The optional where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @param sortSpec The optional sort specification to order the returned records.
     * @return The response from the Vantiq server
     */
    public VantiqResponse select(String resource,
                                 List<String> propSpecs,
                                 Object where,
                                 SortSpec sortSpec) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        if(propSpecs != null) {
            queryParams.put("props", VantiqSession.gson.toJson(propSpecs));
        }
        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }
        if(sortSpec != null) {
            queryParams.put("sort", VantiqSession.gson.toJson(sortSpec.serialize()));
        }

        VantiqResponse response = this.session.get(path, queryParams, null);
        if(response != null) {
            if(response.getBody() instanceof JsonArray) {
                JsonArray arr = (JsonArray) response.getBody();
                List<JsonObject> resultBody = new ArrayList<JsonObject>();
                for(int i=0; i<arr.size(); i++) {
                    resultBody.add((JsonObject) arr.get(i));
                }
                response.setBody(resultBody);
            }
        }
        return response;
    }

    /**
     * Returns the record for the given resource and specified id asynchronously.
     * The response is a single JsonObject.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public void selectOne(String resource,
                         String id,
                         ResponseHandler responseHandler) {
        String path = "/resources/" + resource + "/" + id;
        this.session.get(path, null, responseHandler);
    }

    /**
     * Returns the record for the given resource and specified id synchronously.
     * The response is a single JsonObject.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @return The response from the Vantiq server
     */
    public VantiqResponse selectOne(String resource,
                                    String id) {
        String path = "/resources/" + resource + "/" + id;
        return this.session.get(path, null, null);
    }

    /**
     * This method is similar to {@link #select} except returns only the count of matching records.  The
     * response is an Integer.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param where The optional where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public void count(String resource,
                      Object where,
                      ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");

        // Since we are just counting, we only return "_id" property from server
        queryParams.put("props", VantiqSession.gson.toJson(Collections.singletonList("_id")));

        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }

        this.session.get(path, queryParams, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                String cntValue = response.header("X-Total-Count");
                try {
                    if(cntValue != null) {
                        this.delegate.onSuccess(Integer.parseInt(cntValue), response);
                    } else {
                        this.delegate.onSuccess(0, response);
                    }
                } catch(NumberFormatException ex) {
                    this.delegate.onFailure(ex);
                }
            }
        });
    }

    /**
     * This method is similar to {@link #select} except returns only the count of matching records.  The
     * response is an Integer.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param where The optional where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @return The response from the Vantiq server
     */
    public VantiqResponse count(String resource, Object where) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");

        // Since we are just counting, we only return "_id" property from server
        queryParams.put("props", VantiqSession.gson.toJson(Collections.singletonList("_id")));

        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }

        VantiqResponse response = this.session.get(path, queryParams, null);
        if(response != null) {
            response.setBody(response.getCount());
        }
        return response;
    }

    /**
     * Inserts a new record of the specified resource type asynchronously.  The response is a
     * JsonObject of the record just inserted.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public void insert(String resource,
                       Object object,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;
        this.session.post(path, null, VantiqSession.gson.toJson(object), responseHandler);
    }

    /**
     * Inserts a new record of the specified resource type synchronously.  The response is a
     * JsonObject of the record just inserted.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse insert(String resource,
                                 Object object) {
        String path = "/resources/" + resource;
        return this.session.post(path, null, VantiqSession.gson.toJson(object), null);
    }

    /**
     * Updates an existing record of the specified resource type asynchronously.  The response is a
     * JsonObject of the record just updated.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public void update(String resource,
                       String id,
                       Object object,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource + "/" + id;
        this.session.put(path, null, VantiqSession.gson.toJson(object), responseHandler);
    }

    /**
     * Updates an existing record of the specified resource type synchronously.  The response is a
     * JsonObject of the record just updated.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse update(String resource,
                                 String id,
                                 Object object) {
        String path = "/resources/" + resource + "/" + id;
        return this.session.put(path, null, VantiqSession.gson.toJson(object), null);
    }

    /**
     * Upserts a record of the specified resource type asynchronously.  If the record does not already exist,
     * then a new record is inserted.  If the record does exist, then the record is updated.
     * The existence of the record is defined by the natural key defined on the type.
     *
     * The response is a JsonObject that is the record just upserted.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.  If null,
     *                        then the call is performed synchronously and the response is
     *                        provided as the returned value.
     */
    public void upsert(String resource,
                       Object object,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        // Mongo doesn't like us passing back the _id
        if(object instanceof JsonObject) {
            JsonObject jsonObj = (JsonObject) object;
            if(jsonObj.has("_id")) {
                jsonObj.remove("_id");
            }
        }

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("upsert", "true");

        this.session.post(path, queryParams, VantiqSession.gson.toJson(object), responseHandler);
    }

    /**
     * Upserts a record of the specified resource type synchronously.  If the record does not already exist,
     * then a new record is inserted.  If the record does exist, then the record is updated.
     * The existence of the record is defined by the natural key defined on the type.
     *
     * The response is a JsonObject that is the record just upserted.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param object The object to insert.  This will be converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse upsert(String resource,
                                 Object object) {
        String path = "/resources/" + resource;

        // Mongo doesn't like us passing back the _id
        if(object instanceof JsonObject) {
            JsonObject jsonObj = (JsonObject) object;
            if(jsonObj.has("_id")) {
                jsonObj.remove("_id");
            }
        }

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("upsert", "true");

        return this.session.post(path, queryParams, VantiqSession.gson.toJson(object), null);
    }

    /**
     * Deletes the matching records specified by a where constraint asynchronously.  The response is a boolean
     * indicating the success of the removal.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param where The required where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void delete(String resource,
                       Object where,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");
        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }

        this.session.delete(path, queryParams, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Deletes the matching records specified by a where constraint synchronously.  The response is a boolean
     * indicating the success of the removal.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param where The required where constraint that filters the records returned.  The where is structured
     *              following the structure outline in the
     *              <a href="https://dev.vantiq.com/docs/api/developer.html#api-operations">API Documentation</a>.
     * @return The response from the Vantiq server
     */
    public VantiqResponse delete(String resource,
                                 Object where) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");
        if(where != null) {
            queryParams.put("where", VantiqSession.gson.toJson(where));
        }

        VantiqResponse response = this.session.delete(path, queryParams, null);
        if(response != null && response.isSuccess()) {
            response.setBody(true);
        }
        return response;
    }

    /**
     * Deletes the record for the given resource and specified id asynchronously.   The response is a boolean
     * indicating the success of the removal.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void deleteOne(String resource,
                          String id,
                          ResponseHandler responseHandler) {
        String path = "/resources/" + resource + "/" + id;
        this.session.delete(path, null, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Deletes the record for the given resource and specified id synchronously.   The response is a boolean
     * indicating the success of the removal.
     *
     * @param resource The resource to query.  This can be a {@link Vantiq.SystemResources SystemResources} value or
     *                 a user-defined type name.
     * @param id The key used to lookup the record.  The underlying "_id" can be used.
     * @return The response from the Vantiq server
     */
    public VantiqResponse deleteOne(String resource,
                                    String id) {
        String path = "/resources/" + resource + "/" + id;
        VantiqResponse response = this.session.delete(path, null, null);
        if(response != null && response.isSuccess()) {
            response.setBody(true);
        }
        return response;
    }

    /**
     * Publishes to a specific topic or source asynchronously.  A publish operation is fire-and-forget.
     * The response is a boolean indicating the success of the publish.
     *
     * For sources, the parameters required are source specific and are the same as those required when
     * performing a <code>PUBLISH ... TO SOURCE ... USING</code> params.
     * Please refer to the specific source definition documentation in the
     * <a href="https://dev.vantiq.com/docs/api/index.html">Vantiq API Documentation</a>.
     *
     * @param resource The resource to query.  This must be the value of either
     *                 {@link Vantiq.SystemResources#TOPICS} or {@link Vantiq.SystemResources#SOURCES}.
     * @param id The topic or source to publish.
     * @param payload For a topic, this is the message to publish.  For a source, this is the parameters that
     *                defines the publish.  This will be converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void publish(String resource,
                        String id,
                        Object payload,
                        ResponseHandler responseHandler) {
        if(!SystemResources.SOURCES.value().equals(resource) &&
           !SystemResources.TOPICS.value().equals(resource)) {
            throw new IllegalArgumentException("Only 'sources' and 'topics' support publish");
        }

        String path = "/resources/" + resource + "/" + id;
        this.session.post(path, null, VantiqSession.gson.toJson(payload), new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Publishes to a specific topic or source synchronously.  A publish operation is fire-and-forget.
     * The response is a boolean indicating the success of the publish.
     *
     * For sources, the parameters required are source specific and are the same as those required when
     * performing a <code>PUBLISH ... TO SOURCE ... USING</code> params.
     * Please refer to the specific source definition documentation in the
     * <a href="https://dev.vantiq.com/docs/api/index.html">Vantiq API Documentation</a>.
     *
     * @param resource The resource to query.  This must be the value of either
     *                 {@link Vantiq.SystemResources#TOPICS} or {@link Vantiq.SystemResources#SOURCES}.
     * @param id The topic or source to publish.
     * @param payload For a topic, this is the message to publish.  For a source, this is the parameters that
     *                defines the publish.  This will be converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse publish(String resource,
                                  String id,
                                  Object payload) {
        if(!SystemResources.SOURCES.value().equals(resource) &&
            !SystemResources.TOPICS.value().equals(resource)) {
            throw new IllegalArgumentException("Only 'sources' and 'topics' support publish");
        }

        String path = "/resources/" + resource + "/" + id;
        VantiqResponse response = this.session.post(path, null, VantiqSession.gson.toJson(payload), null);
        if(response != null && response.isSuccess()) {
            response.setBody(true);
        }
        return response;
    }

    /**
     * Executes a specific procedure asynchronously.  An execute operation is request-response.
     * The response is a JsonObject with the result of the procedure.
     *
     * @param procedure The name of the procedure to execute.
     * @param params The arguments for the procedure.  The parameters can be passed as positional
     *               parameters using a JsonArray or as named parameters using a JsonObject.  The params
     *               is converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void execute(String procedure,
                        Object params,
                        ResponseHandler responseHandler) {
        String path = "/resources/" + SystemResources.PROCEDURES.value() + "/" + procedure;
        this.session.post(path, null, VantiqSession.gson.toJson(params), responseHandler);
    }

    /**
     * Executes a specific procedure synchronously.  An execute operation is request-response.
     * The response is a JsonObject with the result of the procedure.
     *
     * @param procedure The name of the procedure to execute.
     * @param params The arguments for the procedure.  The parameters can be passed as positional
     *               parameters using a JsonArray or as named parameters using a JsonObject.  The params
     *               is converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse execute(String procedure,
                                  Object params) {
        String path = "/resources/" + SystemResources.PROCEDURES.value() + "/" + procedure;
        return this.session.post(path, null, VantiqSession.gson.toJson(params), null);
    }

    /**
     * Evaluates a specific analytics model asynchronously.  An evaluation operation is request-response).
     * The response is a JsonObject with the result of the model evaluation.
     *
     * @param modelName The name of the analytics model to evaluate.
     * @param params The arguments for the model.  The parameters are passed as a JSON element
     *               containing the required inputs as defined in the analytics model.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void evaluate(String modelName,
                         Object params,
                         ResponseHandler responseHandler) {
        String path = "/resources/" + SystemResources.ANALYTICS_MODELS.value() + "/" + modelName;
        this.session.post(path, null, VantiqSession.gson.toJson(params), responseHandler);
    }

    /**
     * Evaluates a specific analytics model synchronously.  An evaluation operation is request-response).
     * The response is a JsonObject with the result of the model evaluation.
     *
     * @param modelName The name of the analytics model to evaluate.
     * @param params The arguments for the model.  The parameters are passed as a JSON element
     *               containing the required inputs as defined in the analytics model.
     * @return The response from the Vantiq server
     */
    public VantiqResponse evaluate(String modelName,
                                   Object params) {
        String path = "/resources/" + SystemResources.ANALYTICS_MODELS.value() + "/" + modelName;
        return this.session.post(path, null, VantiqSession.gson.toJson(params), null);
    }

    /**
     * Performs a query operation on the specified source asynchronously.  A query operation is
     * request-response.  The response is a JsonObject with the result of the source query operation.
     *
     * The parameters required are source specific and are the same as those required when performing
     * a <code>SELECT ... FROM SOURCE ... WITH</code> params. Please refer to the specific source
     * definition documentation in the
     * <a href="https://dev.vantiq.com/docs/api/index.html">Vantiq API Documentation</a>.
     *
     * @param source The name of the source to query.
     * @param params The arguments for the query operation.  The params is converted to JSON using Gson.
     * @param responseHandler The response handler that is called upon completion.
     */
    public void query(String source,
                      Object params,
                      ResponseHandler responseHandler) {
        String path = "/resources/sources/" + source + "/query";
        this.session.post(path, null, VantiqSession.gson.toJson(params), responseHandler);
    }

    /**
     * Performs a query operation on the specified source synchronously.  A query operation is
     * request-response.  The response is a JsonObject with the result of the source query operation.
     *
     * The parameters required are source specific and are the same as those required when performing
     * a <code>SELECT ... FROM SOURCE ... WITH</code> params. Please refer to the specific source
     * definition documentation in the
     * <a href="https://dev.vantiq.com/docs/api/index.html">Vantiq API Documentation</a>.
     *
     * @param source The name of the source to query.
     * @param params The arguments for the query operation.  The params is converted to JSON using Gson.
     * @return The response from the Vantiq server
     */
    public VantiqResponse query(String source,
                                Object params) {
        String path = "/resources/sources/" + source + "/query";
        return this.session.post(path, null, VantiqSession.gson.toJson(params), null);
    }

    private boolean verifyAccessToken() throws Exception
    {
        String server = this.session.getServer();
        String path = server + "/api/v1/_status";
        HttpURLConnection connection = null;

        String authValue = "Bearer " + this.session.getAccessToken();

        URL url = new URL(path);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setUseCaches(false);

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", authValue);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            return false;
        }
        return true;
    }

    public JsonObject upload(File file, String mimeType, String documentName) throws Exception
    {
        JsonObject responseObject = new JsonObject();

        //
        //  There is no way to "fail early" with a bad access token when using HttpURLConnection like this, so
        //  we do a quick verification of the access token before doing the real upload. The upload could *still* fail
        //  with a 401 in the next few milliseconds but that's very unlikely. We just want to reduce the chances that we
        //  will do a 50MB upload and find out at the end that it failed with a 401.
        //
        if (!this.verifyAccessToken())
        {
            responseObject.addProperty("statusCode",HttpURLConnection.HTTP_UNAUTHORIZED);
            responseObject.addProperty("message","Unauthorized");
            responseObject.addProperty("code","io.vantiq.client.unauthorized");
            return responseObject;
        }

        responseObject.addProperty("statusCode",0);

        String server = this.session.getServer();
        String path = server + "/api/v1/resources/" + SystemResources.DOCUMENTS.value();
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
        String lineEnd = "\r\n";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024*1024;

        String filefield = "defaultName";
        String authValue = "Bearer " + this.session.getAccessToken();

        FileInputStream fileInputStream = new FileInputStream(file);

        URL url = new URL(path);
        connection = (HttpURLConnection) url.openConnection();
        connection.setChunkedStreamingMode(maxBufferSize);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authValue);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

        //
        //  getOutputStream opens a connection and sends POST and headers
        //
        outputStream = new DataOutputStream(connection.getOutputStream());

        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + documentName + "\"" + lineEnd);
        outputStream.writeBytes("Content-Type: " + mimeType + lineEnd);
        outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
        outputStream.writeBytes(lineEnd);

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0)
        {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            //outputStream.flush();
        }

        outputStream.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd);

        fileInputStream.close();

        //outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        StringBuilder errSB = new StringBuilder();
        String line;

        responseObject.addProperty("statusCode",responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK)
        {
            final InputStream err = connection.getErrorStream();

            if (err != null)
            {
                try
                {
                    try
                    {
                        br = new BufferedReader(new InputStreamReader(err));

                        while ((line = br.readLine()) != null)
                        {
                            errSB.append(line);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        if (br != null)
                        {
                            try
                            {
                                br.close();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                finally
                {
                    err.close();
                }
            }

            String response = errSB.toString();

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(response);
            JsonObject errorObj = null;

            if (jsonElement.isJsonArray())
            {
                JsonArray ja = jsonElement.getAsJsonArray();

                if (ja.size() > 0)
                {
                    errorObj = ja.get(0).getAsJsonObject();
                }
            }
            else
            {
                errorObj = jsonElement.getAsJsonObject();
            }

            responseObject.addProperty("code",errorObj.get("code").getAsString());
            responseObject.addProperty("message",errorObj.get("message").getAsString());
        }
        else
        {
            inputStream = connection.getInputStream();

            try
            {
                br = new BufferedReader(new InputStreamReader(inputStream));

                while ((line = br.readLine()) != null)
                {
                    sb.append(line);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (br != null)
                {
                    try
                    {
                        br.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                inputStream.close();
            }

            String response = sb.toString();
            JsonObject jo = gson.fromJson(response,JsonObject.class);
            responseObject.add("data",jo);
        }

        return responseObject;
    }

    /**
     * Subscribes to a specific topic, source, or type event.  This method uses a
     * WebSocket with the Vantiq server to listen for the specified events.
     *
     * For sources, this will subscribe to message arrival events.  The name of the
     * source is required (e.g. "MySource").
     *
     * For topics, this will subscribe to any messages published on that topic.  The
     * name of the topic is required (e.g. "/foo/bar").
     *
     * For types, this will subscribe to the specified type event.  The name of the
     * type and the operation (i.e. "insert", "update", or "delete") is required.
     *
     * @param resource The resource whose events to subscribe.  This must be the
     *                 value of {@link Vantiq.SystemResources#TOPICS},
     *                 {@link Vantiq.SystemResources#SOURCES}, or
     *                 {@link Vantiq.SystemResources#TYPES}.
     * @param id The id of the resource
     * @param operation Only for "types", the specific operation event to subscribe to.
     * @param callback The callback used when the event is received
     */
    public void subscribe(String resource,
                          String id,
                          TypeOperation operation,
                          SubscriptionCallback callback) {

        String path = "/" + resource + "/" + id;
        if(SystemResources.SOURCES.value().equals(resource) ||
           SystemResources.TOPICS.value().equals(resource)) {
            if(operation != null) {
                throw new IllegalArgumentException("Operation only support for 'types'");
            }
        } else if(SystemResources.TYPES.value().equals(resource)) {
            if(operation == null) {
                throw new IllegalArgumentException("Operation required for 'types'");
            }
            path += "/" + operation.toString().toLowerCase();
        } else {
            throw new IllegalArgumentException("Only 'topics', 'sources' and 'types' support subscribe");
        }

        this.session.subscribe(path, callback);
    }

    /**
     * Unsubscribes to all current subscriptions by closing the WebSocket to the Vantiq
     * server.
     */
    public void unsubscribeAll() {
        this.session.unsubscribeAll();
    }

    //----------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------

    /**
     * Sets the access token for use in future requests to the Vantiq server.  This
     * can be a normal or long-lived token.
     *
     * @param accessToken The access token to set
     */
    public void setAccessToken(String accessToken) {
        this.session.setAccessToken(accessToken);
    }

    /**
     * Returns the current access token.  If not authenticated, this is null.
     *
     * @return The access token used for requests or null if not an authenticated session.
     */
    public String getAccessToken() {
        return this.session.getAccessToken();
    }


}
