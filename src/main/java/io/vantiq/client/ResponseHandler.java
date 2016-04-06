package io.vantiq.client;

import okhttp3.Response;

import java.util.List;

/**
 * Interface to handle the response from the Vantiq server.
 */
public interface ResponseHandler {

    /**
     * This is called when a successful HTTP response is returned from the
     * server.  The type of the body is determined by the Content-Type of the
     * response.  For JSON responses, the body will be a Gson JsonElement.
     * Otherwise, a string is returned.
     *
     * @param body The parsed response from the server.  The type of the
     *             response is determined by SDK method being called.
     * @param response The raw response from the server.  This provides access
     *                 to the HTTP status code, headers, and raw body.
     */
    void onSuccess(Object body, Response response);

    /**
     * Called if a server error is returned (e.g. HTTP 4xx or 5xx status codes), then
     * the status code is returned along with the errors that were found.
     *
     * @param errors The errors returned by the Vantiq server.
     * @param response The raw response from the server.  This provides access
     *                 to the HTTP status code, headers, and raw body.
     */
    void onError(List<VantiqError> errors, Response response);

    /**
     * Called if a client side exception occurs during the request or response
     * processing.
     *
     * @param t The exception thrown during the client-side processing.
     */
    void onFailure(Throwable t);

}
