package io.vantiq.client;

import com.google.gson.JsonObject;
import okhttp3.Response;

import java.util.List;

/**
 * This is the base class for response handlers that stores the
 * response data and provides access to the standard headers.
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
        try {
            // Check for count in header
            String cntValue = response.header("X-Total-Count");
            if (cntValue != null) {
                try {
                    this.count = Integer.parseInt(cntValue);
                } catch (NumberFormatException ex) {
                    /* If not parsable, then just ignore it */
                }
            }

            // Extract content type
            this.contentType = response.header("Content-Type");

            // Extract HTTP status code
            this.statusCode = response.code();

            // Store the response body
            this.body = body;
        } finally {
            // Call completion hook
            this.completionHook(true);
        }
    }

    @Override
    public void onError(List<VantiqError> errors, Response response) {
        this.statusCode = response.code();
        this.errors = errors;
        this.completionHook(false);
    }

    @Override
    public void onFailure(Throwable exception) {
        this.exception = exception;
        this.completionHook(false);
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
        return this.errors != null && this.errors.size() > 0;
    }

    /**
     * Returns true if an exception occurred during the client processing
     * @return true if an exception was thrown
     */
    public boolean hasException() {
        return this.exception != null;
    }

    /**
     * Returns the exception if {@link #hasException} returned true
     * @return The exception that was thrown or null
     */
    public Throwable getException() {
        return this.exception;
    }

    /**
     * Returns the errors if {@link #hasErrors} returned true
     * @return The errors returned from the Vantiq server or null
     */
    public List<VantiqError> getErrors() {
        return this.errors;
    }

    /**
     * Returns the count from the "X-Total-Count" header.  If the
     * header wasn't present, then the count returns -1.
     * @return The returned count or -1 if no count header was present.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Returns the content type from the "Content-Type" HTTP response
     * header.
     * @return The content type of the response
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the HTTP status code returned from the response
     * @return The HTTP status code for the response
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the response body.  If there was an exception or error, then
     * no body will exist.
     * @return The parsed response body
     */
    public Object getBody() {
        return this.body;
    }

    /**
     * Returns the body as a JsonObject or null if the body is not a JsonObject
     * @return The parsed body as a JsonObject or null if not a JsonObject
     */
    public JsonObject getBodyAsJsonObject() {
        return (this.body instanceof JsonObject ? (JsonObject) body : null);
    }

    /**
     * Returns the body as a list of JsonObjects or null if the body is not a List.
     * @return The parsed body as a List of JsonObjects or null if not a list
     */
    @SuppressWarnings("unchecked")
    public List<JsonObject> getBodyAsList() {
        return (this.body instanceof List ? (List<JsonObject>) body : null);
    }

    /**
     * Returns the body as a String or null if the body is not a String
     * @return The parsed body as a String or null if not a String
     */
    public String getBodyAsString() {
        return (this.body instanceof String ? (String) body : null);
    }

    /**
     * Returns the body as an int or null if the body is not an int
     * @return The parsed body as a int or null if not an int
     */
    public int getBodyAsInt() {
        return (this.body instanceof Integer ? (Integer) body : null);
    }

    /**
     * Returns the body as a boolean or null if the body is not a boolean
     * @return The parsed body as a boolean or null if not a boolean
     */
    public boolean getBodyAsBoolean() {
        return (this.body instanceof Boolean ? (Boolean) body : null);
    }
}
