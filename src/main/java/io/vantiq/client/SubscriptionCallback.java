package io.vantiq.client;

/**
 * Interface that listens for events subscribed to from a
 * Vantiq server.
 */
public interface SubscriptionCallback {

    /**
     * Called once the connection has been established and the
     * subscription has been acknowledged by the Vantiq server.
     */
    void onConnect();

    /**
     * Called for every matching event that occurs
     *
     * @param message The information associated with the event
     */
    void onMessage(SubscriptionMessage message);

    /**
     * Called whenever an error occurs that does not arise from
     * an exception, such as if a non-success response is provided.
     *
     * @param error The error message
     */
    void onError(String error);

    /**
     * Called whenever an exception occurs during the subscription
     * processing.
     *
     * @param t The exception thrown
     */
    void onFailure(Throwable t);
}
