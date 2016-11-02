package io.vantiq.client;

import okhttp3.Response;

import java.util.List;

/**
 * Call handler used for unit test to record what was returned
 */
public class UnitTestResponseHandler extends BaseResponseHandler {

    public boolean success = false;
    public boolean error = false;
    public boolean failure = false;

    public UnitTestResponseHandler reset() {
        this.success = false;
        this.error = false;
        this.failure = false;
        return this;
    }

    @Override
    public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        this.success = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onError(List<VantiqError> errors, Response response) {
        super.onError(errors, response);
        this.error = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        super.onFailure(t);
        this.failure = true;
        synchronized (this) {
            notify();
        }
    }

    public String toString() {
        if (this.success) {
            return "SUCCESS: " + this.getBody();
        } else if (this.error) {
            return "ERROR: [" + this.getStatusCode() + "]: " + this.getErrors();
        } else if (this.failure) {
            return "FAILURE: " + this.getException();
        } else {
            return "UNKNOWN STATE";
        }
    }

}
