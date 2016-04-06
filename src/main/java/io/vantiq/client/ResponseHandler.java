package io.vantiq.client;

import okhttp3.Response;

import java.util.List;

/**
 * Interface to handle the response from the server
 */
public interface ResponseHandler {

    /**
     * This is called when a successful HTTP response is returned from the
     * server.  The type of the body is determined by the Content-Type of the
     * response.  For JSON responses, the body will be a Gson JsonElement.
     * Otherwise, a string is returned.
     */
    void onSuccess(Object body, Response response);

    /**
     * Called if a server error is returned (e.g. HTTP 4xx or 5xx status codes), then
     * the status code is returned along with the errors that were found.
     */
    void onError(List<VantiqError> errors, Response response);

    /**
     * Called if a client side exception occurs during the request or response
     * processing
     */
    void onFailure(Throwable t);

}
