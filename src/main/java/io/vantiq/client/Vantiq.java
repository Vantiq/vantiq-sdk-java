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
        DOCUMENTS("documents"),
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

        private String resource;

        private SystemResources(String resource) {
            this.resource = resource;
        }

        public String getResource() {
            return this.resource;
        }
    }

    private static Gson gson = new Gson();

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
     */
    public boolean isAuthenticated() {
        return this.session.isAuthenticated();
    }

    /**
     * Authenticates this Vantiq instance using the given credentials.  The response
     * handler "onSuccess" will return true if the authentication was successful.
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
     * Performs a query to return matching records.  The response body will
     * be a List of JsonObject objects.
     *
     * @param resource
     * @param propSpecs
     * @param where
     * @param sortSpec
     * @param responseHandler
     */
    public void select(String resource,
                       List<String> propSpecs,
                       Object where,
                       SortSpec sortSpec,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        if(propSpecs != null) {
            queryParams.put("props", gson.toJson(propSpecs));
        }
        if(where != null) {
            queryParams.put("where", gson.toJson(where));
        }
        if(sortSpec != null) {
            queryParams.put("sort", gson.toJson(sortSpec.serialize()));
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
     * Returns the record for the given resource and specified id.
     *
     * @param resource
     * @param id
     * @param responseHandler
     */
    public void selectOne(String resource,
                          String id,
                          ResponseHandler responseHandler) {
        String path = "/resources/" + resource + "/" + id;
        this.session.get(path, null, responseHandler);
    }

    /**
     * Performs a query to count the matching records.
     *
     * @param resource
     * @param where
     * @param responseHandler
     */
    public void count(String resource,
                      Object where,
                      ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");

        // Since we are just counting, we only return "_id" property from server
        queryParams.put("props", gson.toJson(Collections.singletonList("_id")));

        if(where != null) {
            queryParams.put("where", gson.toJson(where));
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
     * Inserts a new resource record
     *
     * @param resource
     * @param object
     * @param responseHandler
     */
    public void insert(String resource,
                       Object object,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;
        this.session.post(path, null, gson.toJson(object), responseHandler);
    }

    /**
     * Updates an existing resource record
     *
     * @param resource
     * @param key
     * @param object
     * @param responseHandler
     */
    public void update(String resource,
                       String key,
                       Object object,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource + "/" + key;
        this.session.put(path, null, gson.toJson(object), responseHandler);
    }

    /**
     * Upsert a resource.  If the resource already exists (as
     * defined by a natural key), then update it.  Otherwise,
     * insert a new record.
     *
     * @param resource
     * @param object
     * @param responseHandler
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

        this.session.post(path, queryParams, gson.toJson(object), responseHandler);
    }

    /**
     * Deletes a number of records matching the given where clause.
     *
     * @param resource
     * @param where
     * @param responseHandler
     */
    public void delete(String resource,
                       Object where,
                       ResponseHandler responseHandler) {
        String path = "/resources/" + resource;

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("count", "true");
        if(where != null) {
            queryParams.put("where", gson.toJson(where));
        }

        this.session.delete(path, queryParams, new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Deletes the record for the given resource and specified id.
     *
     * @param resource
     * @param id
     * @param responseHandler
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
     * Publishes onto a topic or a source
     *
     * @param resource
     * @param id
     * @param payload
     * @param responseHandler
     */
    public void publish(String resource,
                        String id,
                        JsonObject payload,
                        ResponseHandler responseHandler) {
        if(!SystemResources.SOURCES.getResource().equals(resource) &&
           !SystemResources.TOPICS.getResource().equals(resource)) {
            throw new IllegalArgumentException("Only 'sources' and 'topics' support publish");
        }

        String path = "/resources/" + resource + "/" + id;
        this.session.post(path, null, gson.toJson(payload), new PassThruResponseHandler(responseHandler) {
            @Override
            public void onSuccess(Object body, Response response) {
                this.delegate.onSuccess(true, response);
            }
        });
    }

    /**
     * Executes a specified procedure with the given arguments
     *
     * @param procedure
     * @param params
     * @param responseHandler
     */
    public void execute(String procedure,
                        JsonObject params,
                        ResponseHandler responseHandler) {
        String path = "/resources/procedures/" + procedure;
        this.session.post(path, null, gson.toJson(params), responseHandler);
    }

    /**
     * Queries a specified source
     *
     * @param source
     * @param params
     * @param responseHandler
     */
    public void query(String source,
                      JsonObject params,
                      ResponseHandler responseHandler) {
        String path = "/resources/sources/" + source + "/query";
        this.session.post(path, null, gson.toJson(params), responseHandler);
    }
}
