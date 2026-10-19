package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByConversationIdAndSenderUsernameAndSenderClientDeviceKeyAndClientMessageId(
            Long conversationId, String senderUsername, String senderClientDeviceKey, String clientMessageId);

    List<ChatMessage> findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(
            Long conversationId, Long afterSeq, Pageable pageable);

    List<ChatMessage> findByConversationIdOrderByServerSeqDesc(Long conversationId, Pageable pageable);

    @Query("select coalesce(max(m.serverSeq), 0) from ChatMessage m where m.conversationId = :conversationId")
    Long findMaxServerSeq(@Param("conversationId") Long conversationId);

    long countByConversationIdAndServerSeqGreaterThan(Long conversationId, Long afterSeq);

    @Query("select m.createdAt from ChatMessage m where m.createdAt >= :from")
    List<LocalDateTime> findCreatedAtAfter(@Param("from") LocalDateTime from);
}
