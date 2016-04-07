package io.vantiq.examplesdkclient;

import android.app.Application;

import io.vantiq.client.Vantiq;

/**
 * Example application class that holds the Vantiq SDK instance
 */
public class ExampleApplication extends Application {

    private Vantiq vantiq;

    public void setVantiq(Vantiq vantiq) {
        this.vantiq = vantiq;
    }

    public Vantiq getVantiq() {
        return this.vantiq;
    }

}
