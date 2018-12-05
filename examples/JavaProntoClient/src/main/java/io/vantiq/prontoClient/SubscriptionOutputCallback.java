package io.vantiq.prontoClient;

import java.io.IOException;

import javax.websocket.Session;
import com.google.gson.internal.LinkedTreeMap;

import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;

import io.vantiq.prontoClient.servlet.*;

public class SubscriptionOutputCallback implements SubscriptionCallback {
    
    // Websocket session used to send messages to client
    Session wsSession;
    String sessionID;
    
    public SubscriptionOutputCallback(String sessionID) {
        this.sessionID = sessionID;
    }
    
    @Override
    public void onConnect() {
        // (Uncomment to debug)
        //System.out.println("Connected Successfully");
    }

    @Override
    public void onMessage(SubscriptionMessage message) { 
        // Getting message body, which contains the event data
        LinkedTreeMap<?,?> bodyTree = (LinkedTreeMap<?,?>) message.getBody();
        
        // Getting the WebSocket Session for our given Session ID
        if (wsSession == null) {
            wsSession = LiveViewServlet.socketMap.get(sessionID);
        }
        
        // Make sure we have a WebSocket Session and it is open
        if (wsSession != null && wsSession.isOpen()) {
            try {
                // Send the data to the client (data is under "value" field in message body)
                wsSession.getBasicRemote().sendText(bodyTree.get("value").toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(String error) {
        // (Uncomment to debug)
        //System.out.println("Error: " + error);
    }

    @Override
    public void onFailure(Throwable t) {
        // (Uncomment to debug)
        //t.printStackTrace();
    }
}
