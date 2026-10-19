package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatAccountGroupPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatAccountGroupPermissionRepository extends JpaRepository<ChatAccountGroupPermission, Long> {

    Optional<ChatAccountGroupPermission> findByUsername(String username);

    List<ChatAccountGroupPermission> findAllByOrderByUsernameAsc();

    void deleteByUsername(String username);
}
