package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatOneTimePreKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatOneTimePreKeyRepository extends JpaRepository<ChatOneTimePreKey, Long> {

    Optional<ChatOneTimePreKey> findByUsernameAndDeviceIdAndKeyId(String username, Integer deviceId, Integer keyId);

    List<ChatOneTimePreKey> findByUsernameAndDeviceIdOrderByKeyIdAsc(String username, Integer deviceId);

    long countByUsernameAndDeviceIdAndUsedFalse(String username, Integer deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from ChatOneTimePreKey k where k.username = :username and k.deviceId = :deviceId and k.used = false order by k.keyId asc")
    List<ChatOneTimePreKey> findUnusedForUpdate(@Param("username") String username,
                                                @Param("deviceId") Integer deviceId);

    void deleteByUsernameAndDeviceId(String username, Integer deviceId);

    void deleteByUsername(String username);
}
