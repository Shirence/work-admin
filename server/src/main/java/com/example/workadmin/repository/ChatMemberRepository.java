package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByConversationIdOrderByJoinedAtAsc(Long conversationId);

    List<ChatMember> findByConversationIdInOrderByJoinedAtAsc(Collection<Long> conversationIds);

    List<ChatMember> findByUsernameOrderByJoinedAtAsc(String username);

    Optional<ChatMember> findByConversationIdAndUsername(Long conversationId, String username);

    boolean existsByConversationIdAndUsername(Long conversationId, String username);

    long countByConversationId(Long conversationId);

    long deleteByConversationIdAndUsername(Long conversationId, String username);

    void deleteByConversationId(Long conversationId);
}
