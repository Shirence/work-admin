package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatConversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByPrivateKey(String privateKey);

    List<ChatConversation> findByIdIn(Collection<Long> ids);

    /**
     * 消息发送时锁定会话行，串行分配 serverSeq，并让同一 clientMessageId 的并发重发
     * 在锁内被幂等识别。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatConversation c where c.id = :id")
    Optional<ChatConversation> findByIdForUpdate(@Param("id") Long id);
}
