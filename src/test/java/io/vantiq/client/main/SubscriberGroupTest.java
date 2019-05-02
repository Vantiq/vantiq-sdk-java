package io.vantiq.client.main;

import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import io.vantiq.client.Vantiq;

import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;

/**
 * Example client that subscribes to a specific topic and prints out events
 */
public class SubscriberGroupTest {

    private static void println(String msg) {
        System.out.println(new Date() + " - " + msg);
    }

    private static void errorln(String err) {
        System.err.println(new Date() + " - " + err);
    }

    public static void main(String... args) throws Exception {
        String server = "http://localhost:8080";
        // Replace with a valid token when running this test!
        String accessToken = "VxUpwBZi2z3sbJTR91Zdd1p7XuubfFhWM86R16vorqs=";

        Vantiq vantiq1 = new Vantiq(server);
        vantiq1.setAccessToken(accessToken);

        HashMap<String, String> subOptions = new HashMap<>();
        subOptions.put("subscriberGroup", "test");

        vantiq1.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test", null, new SubscriptionCallback() {

            @Override
            public void onConnect() {
                println("Subscription created.");
            }

            @Override
            public void onMessage(SubscriptionMessage message) {
                println("1: " + message.getBody().toString());
            }

            @Override
            public void onError(String error) {
                errorln(error);
            }

            @Override
            public void onFailure(Throwable t) {
                if(t instanceof SocketTimeoutException) {
                    errorln("WebSocket timed out.");
                    System.exit(2);
                } else {
                    errorln(t.toString());
                    errorln(t.getMessage());
                    t.printStackTrace();
                }
            }
        }, subOptions);

        Vantiq vantiq2 = new Vantiq(server);
        vantiq2.setAccessToken(accessToken);

        vantiq2.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test", null, new SubscriptionCallback() {

            @Override
            public void onConnect() {
                println("Subscription created.");
            }

            @Override
            public void onMessage(SubscriptionMessage message) {
                println("2: " + message.getBody().toString());
            }

            @Override
            public void onError(String error) {
                errorln(error);
            }

            @Override
            public void onFailure(Throwable t) {
                if(t instanceof SocketTimeoutException) {
                    errorln("WebSocket timed out.");
                    System.exit(2);
                } else {
                    errorln(t.toString());
                    errorln(t.getMessage());
                    t.printStackTrace();
                }
            }
        }, subOptions);

    }
}
