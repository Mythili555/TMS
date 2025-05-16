package com.fyndus.messaging_service.repository;

import com.fyndus.messaging_service.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

}
