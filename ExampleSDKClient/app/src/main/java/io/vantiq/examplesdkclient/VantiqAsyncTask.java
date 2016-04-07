package io.vantiq.examplesdkclient;

import android.os.AsyncTask;

import io.vantiq.client.BaseResponseHandler;
import io.vantiq.client.Vantiq;

/**
 * Abstract AsyncTask class that provides an easy mechanism to issue requests
 * to the Vantiq server.  Like other async tasks, this should only be used once.
 */
public abstract class VantiqAsyncTask<Params,Progress,Result> extends AsyncTask<Params,Progress,Result> {

    private Vantiq vantiq;
    private BaseResponseHandler handler;
    private boolean done = false;

    public VantiqAsyncTask(Vantiq vantiq) {
        this.vantiq = vantiq;

        // Provide the response handler that simply indicates when the response has finished
        this.handler = new BaseResponseHandler() {
            @Override public void completionHook(boolean success) {
                VantiqAsyncTask.this.done = true;
            }
        };
    }

    /**
     * Abstract method that must be overridden by subclasses to issue a Vantiq request.
     * @param handler The response handler
     */
    abstract protected void doRequest(Vantiq vantiq, BaseResponseHandler handler);

    /**
     * Abstract method that must be overridden by subclasses to return the result.
     * @param handler The response handler
     * @return The response of this async task
     */
    abstract protected Result prepareResult(BaseResponseHandler handler);

    @Override
    protected final Result doInBackground(Params... params) {

        // Issue request to the server
        doRequest(this.vantiq, this.handler);

        // Wait for the request to be completed
        try {
            while (!this.done) {
                Thread.sleep(100);
            }
        } catch(InterruptedException ex) {
            /* If interrupted, then just drop out */
        }

        return prepareResult(this.handler);
    }

}
