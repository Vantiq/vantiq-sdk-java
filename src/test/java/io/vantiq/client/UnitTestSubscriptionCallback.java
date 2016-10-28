package io.vantiq.client;

/**
 * Subscription callback handler that notifies whenever a message is received.
 */
public class UnitTestSubscriptionCallback implements SubscriptionCallback {

    private SubscriptionMessage message;
    private Throwable cause;
    private String error;

    private boolean fired = false;

    @Override
    public void onConnect() {
        this.fired = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onMessage(SubscriptionMessage message) {
        this.fired = true;
        this.message = message;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onError(String error) {
        this.fired = true;
        this.error = error;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        this.fired = true;
        this.cause = t;
        synchronized (this) {
            notify();
        }
    }

    public SubscriptionMessage getMessage() {
        return this.message;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getError() {
        return this.error;
    }

    public synchronized void waitForCompletion(int timeout) throws InterruptedException {
        if(!this.fired) {
            wait(timeout);
        }
    }

    public synchronized void waitForCompletion() throws InterruptedException {
        waitForCompletion(2000);
    }

    public void reset() {
        this.message = null;
        this.cause = null;
        this.error = null;
        this.fired = false;
    }

    public boolean hasFired() {
        return this.fired;
    }

    public String toString() {
        if(this.message != null) {
            return "Message: " + this.message.toString();
        } else if (this.error != null) {
            return "Error: " + this.error;
        } else if (this.cause != null) {
            return "Failure: " + this.cause.toString();
        } else {
            return "Callback not called";
        }
    }
}
