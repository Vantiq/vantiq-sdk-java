package io.vantiq.client;

import com.google.gson.JsonObject;
import okhttp3.Response;

import java.util.List;

/**
 * Base class for response handlers that provide default
 * behavior.
 */
public class BaseResponseHandler implements ResponseHandler {

    private int count = -1;
    private int statusCode;
    private String contentType;
    private Object body;
    private Throwable exception;
    private List<VantiqError> errors;

    @Override
    public void onSuccess(Object body, Response response) {
        // Check for count in header
        String cntValue = response.header("X-Total-Count");
        if(cntValue != null) {
            try {
                this.count = Integer.parseInt(cntValue);
            } catch(NumberFormatException ex) {
                /* If not parsable, then just ignore it */
            }
        }

        // Extract content type
        this.contentType = response.header("Content-Type");

        // Extract HTTP status code
        this.statusCode = response.code();

        // Store the response body
        this.body = body;
    }

    @Override
    public void onError(List<VantiqError> errors, Response response) {
        this.statusCode = response.code();
        this.errors = errors;
    }

    @Override
    public void onFailure(Throwable exception) {
        this.exception = exception;
    }

    /**
     * Returns true if at least one error was returned from the server
     */
    public boolean hasErrors() {
        return this.errors != null && this.errors.size() > 0;
    }

    /**
     * Returns true if an exception occurred during the client processing
     */
    public boolean hasException() {
        return this.exception != null;
    }

    /**
     * Returns the exception if {@link #hasException} returned true
     */
    public Throwable getException() {
        return this.exception;
    }

    /**
     * Returns the errors if {@link #hasErrors} returned true
     */
    public List<VantiqError> getErrors() {
        return this.errors;
    }

    /**
     * Returns the count from the "X-Total-Count" header.  If the
     * header wasn't present, then the count returns -1.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Returns the content type for the response
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the HTTP status code returned from the response
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the response body.  If there was an exception or error, then
     * no body will exist.
     */
    public Object getBody() {
        return this.body;
    }

    /**
     * Returns the body as a JsonObject or null if the body is not a JsonObject
     */
    public JsonObject getBodyAsJsonObject() {
        return (this.body instanceof JsonObject ? (JsonObject) body : null);
    }

    /**
     * Returns the body as a list of JsonObjects or null if the body is not a List.
     */
    public List<JsonObject> getBodyAsList() {
        return (this.body instanceof List ? (List<JsonObject>) body : null);
    }

    /**
     * Returns the body as a String or null if the body is not a String
     */
    public String getBodyAsString() {
        return (this.body instanceof String ? (String) body : null);
    }

    /**
     * Returns the body as an int or null if the body is not an int
     */
    public int getBodyAsInt() {
        return (this.body instanceof Integer ? (Integer) body : null);
    }

    /**
     * Returns the body as a boolean or null if the body is not a boolean
     */
    public boolean getBodyAsBoolean() {
        return (this.body instanceof Boolean ? (Boolean) body : null);
    }
}
