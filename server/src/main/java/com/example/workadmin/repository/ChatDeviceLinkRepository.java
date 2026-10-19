package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatDeviceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatDeviceLinkRepository extends JpaRepository<ChatDeviceLink, Long> {

    Optional<ChatDeviceLink> findByUsernameAndNewDeviceKey(String username, String newDeviceKey);

    List<ChatDeviceLink> findByUsernameAndStatusOrderByRequestedAtAsc(String username, String status);

    long countByUsernameAndStatus(String username, String status);

    void deleteByUsername(String username);
}
