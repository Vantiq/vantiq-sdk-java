package io.vantiq.prontoClient;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.servlet.http.HttpSession;

import io.vantiq.prontoClient.servlet.*;

@ServerEndpoint(value="/websocket",
                configurator= GetHttpSessionConfigurator.class)
public class WebSocketServer {
    
    // Using configurator to get the HTTP Session
    
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // Mapping the current WebSocket Session to the HTTP Session
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        LiveViewServlet.socketMap.put(httpSession.getId(), session);
    }
    
    @OnClose
    public void onClose(Session session) {
        // (Uncomment to debug)
        //System.out.println("WS Closed: " + session.getId());
    }
}
