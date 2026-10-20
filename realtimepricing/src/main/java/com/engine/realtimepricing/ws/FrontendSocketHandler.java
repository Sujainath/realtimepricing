package com.engine.realtimepricing.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

@Component
public class FrontendSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new ArrayList<>();

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public synchronized void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : new ArrayList<>(sessions)) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception e) {
                    sessions.remove(session);
                }
            }
        }
    }
}