package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatEnvelope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChatEnvelopeRepository extends JpaRepository<ChatEnvelope, Long> {

    List<ChatEnvelope> findByMessageIdInAndTargetDeviceKey(Collection<Long> messageIds, String targetDeviceKey);

    List<ChatEnvelope> findByMessageIdIn(Collection<Long> messageIds);

    long countByMessageId(Long messageId);

    List<ChatEnvelope> findByTargetUsernameAndTargetDeviceKeyOrderByMessageIdAsc(String targetUsername,
                                                                                 String targetDeviceKey);
}
