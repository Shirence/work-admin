package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatDeviceGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatDeviceGrantRepository extends JpaRepository<ChatDeviceGrant, Long> {

    List<ChatDeviceGrant> findByToDeviceKeyAndConsumedFalseOrderByCreatedAtAsc(String toDeviceKey);

    List<ChatDeviceGrant> findByUsernameAndToDeviceKeyAndConsumedFalseOrderByCreatedAtAsc(String username,
                                                                                          String toDeviceKey);

    List<ChatDeviceGrant> findByLinkId(Long linkId);

    void deleteByUsername(String username);

    void deleteByToDeviceKey(String toDeviceKey);
}
