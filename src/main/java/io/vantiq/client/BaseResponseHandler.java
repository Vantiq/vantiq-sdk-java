package io.vantiq.client;

import com.google.gson.JsonObject;
import okhttp3.Response;

import java.util.List;

/**
 * This is the base class for response handlers that stores the
 * response data and provides access to the standard headers.
 */
public class BaseResponseHandler implements ResponseHandler {

    private VantiqResponse response;

    @Override
    public void onSuccess(Object body, Response response) {
        try {
            this.response = new VantiqResponse(body, response);
        } finally {
            this.completionHook(true);
        }
    }

    @Override
    public void onError(List<VantiqError> errors, Response response) {
        try {
            this.response = new VantiqResponse(errors, response);
        } finally {
            this.completionHook(false);
        }
    }

    @Override
    public void onFailure(Throwable exception) {
        try {
            this.response = new VantiqResponse(exception);
        } finally {
            this.completionHook(false);
        }
    }

    /**
     * Returns the response
     * @return The response from the Vantiq server.
     */
    public VantiqResponse getResponse() {
        return this.response;
    }

    /**
     * Method that is called when the response has completed either
     * successfully, in error, or through an exception.
     *
     * @param success true if onSuccess was called, otherwise, false
     */
    public void completionHook(boolean success) {}

    /**
     * Returns true if at least one error was returned from the server
     * @return true if at least one error exists
     */
    public boolean hasErrors() {
        return this.response != null && this.response.hasErrors();
    }

    /**
     * Returns true if an exception occurred during the client processing
     * @return true if an exception was thrown
     */
    public boolean hasException() {
        return this.response != null && this.response.hasException();
    }

    /**
     * Returns the exception if {@link #hasException} returned true
     * @return The exception that was thrown or null
     */
    public Throwable getException() {
        return this.response != null ? this.response.getException() : null;
    }

    /**
     * Returns the errors if {@link #hasErrors} returned true
     * @return The errors returned from the Vantiq server or null
     */
    public List<VantiqError> getErrors() {
        return this.response != null ? this.response.getErrors() : null;
    }

    /**
     * Returns the count from the "X-Total-Count" header.  If the
     * header wasn't present, then the count returns -1.
     * @return The returned count or -1 if no count header was present.
     */
    public int getCount() {
        return this.response != null ? this.response.getCount() : -1;
    }

    /**
     * Returns the content type from the "Content-Type" HTTP response
     * header.
     * @return The content type of the response
     */
    public String getContentType() {
        return this.response != null ? this.response.getContentType() : null;
    }

    /**
     * Returns the HTTP status code returned from the response
     * @return The HTTP status code for the response
     */
    public int getStatusCode() {
        return this.response != null ? this.response.getStatusCode() : -1;
    }

    /**
     * Returns the response body.  If there was an exception or error, then
     * no body will exist.
     * @return The parsed response body
     */
    public Object getBody() {
        return this.response != null ? this.response.getBody() : null;
    }

    /**
     * Returns the body as a JsonObject or null if the body is not a JsonObject
     * @return The parsed body as a JsonObject or null if not a JsonObject
     */
    public JsonObject getBodyAsJsonObject() {
        Object body = getBody();
        return (body instanceof JsonObject ? (JsonObject) body : null);
    }

    /**
     * Returns the body as a list of JsonObjects or null if the body is not a List.
     * @return The parsed body as a List of JsonObjects or null if not a list
     */
    @SuppressWarnings("unchecked")
    public List<JsonObject> getBodyAsList() {
        Object body = getBody();
        return (body instanceof List ? (List<JsonObject>) body : null);
    }

    /**
     * Returns the body as a String or null if the body is not a String
     * @return The parsed body as a String or null if not a String
     */
    public String getBodyAsString() {
        Object body = getBody();
        return (body instanceof String ? (String) body : null);
    }

    /**
     * Returns the body as an int or 0 if the body is not an int
     * @return The parsed body as a int or 0 if not an int
     */
    public int getBodyAsInt() {
        Object body = getBody();
        return (body instanceof Integer ? (Integer) body : 0);
    }

    /**
     * Returns the body as a boolean or false if the body is not a boolean
     * @return The parsed body as a boolean or false if not a boolean
     */
    public boolean getBodyAsBoolean() {
        Object body = getBody();
        return (body instanceof Boolean ? (Boolean) body : false);
    }
}
