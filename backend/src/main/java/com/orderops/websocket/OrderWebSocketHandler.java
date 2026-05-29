package com.orderops.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        // Send initial greeting or connection confirmation
        session.sendMessage(new TextMessage("{\"type\":\"SYSTEM\",\"message\":\"Connected to OrderOps Live Gateway\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    /**
     * Broadcasts a message to all active WebSocket clients.
     * @param type The category of the update (e.g. "ORDER_UPDATE", "DRIVER_UPDATE", "AGENT_LOG")
     * @param data The payload object to be serialized to JSON.
     */
    public void broadcast(String type, Object data) {
        Map<String, Object> envelope = Map.of(
            "type", type,
            "data", data,
            "timestamp", System.currentTimeMillis()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            TextMessage message = new TextMessage(jsonPayload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to send message to session: " + session.getId() + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to serialize or broadcast message: " + e.getMessage());
        }
    }
}
