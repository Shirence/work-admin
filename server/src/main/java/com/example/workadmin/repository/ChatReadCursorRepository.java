package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatReadCursor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatReadCursorRepository extends JpaRepository<ChatReadCursor, Long> {

    Optional<ChatReadCursor> findByConversationIdAndDeviceKey(Long conversationId, String deviceKey);

    List<ChatReadCursor> findByUsernameAndDeviceKey(String username, String deviceKey);

    void deleteByConversationIdAndDeviceKey(Long conversationId, String deviceKey);

    void deleteByConversationIdAndUsername(Long conversationId, String username);
}
