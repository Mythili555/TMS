package com.fyndus.messaging_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyndus.messaging_service.entity.Message;
import com.fyndus.messaging_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageListener {

    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;

    @KafkaListener(topics = "service-request-created", groupId = "receiver-group")
    public void receive(String request) throws JsonProcessingException {
        try {
            JsonNode jsonNode = objectMapper.readTree(request);
            UUID id = UUID.fromString(jsonNode.get("id").asText());
            UUID tenantId = UUID.fromString(jsonNode.get("tenantId").asText());
            UUID ownerId = UUID.fromString(jsonNode.get("ownerId").asText());
            String description = jsonNode.get("description").asText();
            // Save to DB
            Message message = new Message();
            message.setFromId(tenantId);
            message.setToId(ownerId);
            message.setMessage(description);
            message.setTime(LocalDateTime.now());
            this.messageRepository.save(message);

        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}