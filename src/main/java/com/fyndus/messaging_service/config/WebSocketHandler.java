package com.fyndus.messaging_service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fyndus.messaging_service.repository.MessageRepository;
import com.fyndus.messaging_service.entity.Message;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final MessageRepository messageRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String query = uri != null ? uri.getQuery() : null;

        if (query == null || !query.startsWith("userId=")) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing userId"));
            return;
        }

        String userIdStr = query.split("=")[1];
        UUID userId;

        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid userId format"));
            return;
        }
        System.out.println("Looking for user in DB with ID: " + userId);
        // Save session
        session.getAttributes().put("userId", userId.toString());
        userSessions.put(String.valueOf(userId), session);
        System.out.println("User connected: " + userId);
    }
    private String getUserId(WebSocketSession session) {
        String query = Objects.requireNonNull(session.getUri()).getQuery(); // e.g., userId=UUID
        if (query != null && query.startsWith("userId=")) {
            return query.split("=")[1];
        }
        return null;
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());

        String toUserId = jsonNode.get("to").asText();
        String textMessage = jsonNode.get("message").asText();

        String fromUserId = (String) session.getAttributes().get("userId");

        Message chatMessage = new Message();
        chatMessage.setFromId(UUID.fromString(fromUserId));
        chatMessage.setToId(UUID.fromString(toUserId));
        chatMessage.setMessage(textMessage);
        chatMessage.setTime(LocalDateTime.now());
        messageRepository.save(chatMessage);

        WebSocketSession targetSession = userSessions.get(toUserId);

        if (targetSession == null || !targetSession.isOpen()) {
            session.sendMessage(new TextMessage("User " + toUserId + " is not connected."));
            return;
        }

        ObjectNode outgoingMessage = objectMapper.createObjectNode();
        outgoingMessage.put("from", fromUserId);
        outgoingMessage.put("message", textMessage);

        targetSession.sendMessage(new TextMessage(outgoingMessage.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userIdStr = (String) session.getAttributes().get("userId");
        UUID userId = UUID.fromString(userIdStr);
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("User disconnected: " + userId);
        }
    }
}
