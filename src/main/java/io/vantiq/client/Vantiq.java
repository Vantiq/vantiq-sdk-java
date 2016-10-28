package io.vantiq.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vantiq.client.internal.VantiqSession;
import okhttp3.Response;

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
