package io.vantiq.client.main;

import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqResponse;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Date;

/**
 * Example client that subscribes to a specific topic and prints out events
 */
public class SubscribeTopic {

    private static void println(String msg) {
        System.out.println(new Date() + " - " + msg);
    }

    private static void errorln(String err) {
        System.err.println(new Date() + " - " + err);
    }

    public static void main(String... args) throws Exception {
        if(args.length != 4) {
            System.err.println("Usage: java " + SubscribeTopic.class.getName() + " <serverUrl> <username> <password> <topic>");
            System.exit(1);
        }

        String server = args[0];
        String username = args[1];
        String password = args[2];

        Vantiq vantiq = new Vantiq(args[0]);
        VantiqResponse resp = vantiq.authenticate(args[1], args[2]);
        if(!resp.isSuccess()) {
            throw new RuntimeException(resp.toString());
        }

        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), args[3], null, new SubscriptionCallback() {

            @Override
            public void onConnect() {
                println("Subscription created.");
            }

            @Override
            public void onMessage(SubscriptionMessage message) {
                println(message.getBody().toString());
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
        });

    }
}
