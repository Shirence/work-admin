package com.example.workadmin.repository;

import com.example.workadmin.entity.ChatDevice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatDeviceRepository extends JpaRepository<ChatDevice, Long> {

    List<ChatDevice> findByUsernameOrderByDeviceIdAsc(String username);

    List<ChatDevice> findByUsernameAndStatusOrderByDeviceIdAsc(String username, String status);

    List<ChatDevice> findByUsernameInAndStatusOrderByUsernameAscDeviceIdAsc(Collection<String> usernames, String status);

    Optional<ChatDevice> findByUsernameAndClientDeviceKey(String username, String clientDeviceKey);

    Optional<ChatDevice> findByUsernameAndDeviceId(String username, Integer deviceId);

    Optional<ChatDevice> findFirstByUsernameAndStatusOrderByDeviceIdAsc(String username, String status);

    long countByUsername(String username);

    long countByUsernameAndStatus(String username, String status);

    long countByStatus(String status);

    @Query("select coalesce(max(d.deviceId), 0) from ChatDevice d where d.username = :username")
    Integer findMaxDeviceId(@Param("username") String username);

    /**
     * 串行化同一账号的设备注册，避免并发分配相同 deviceId。
     * 注意：账号还没有任何设备时没有行可锁，仍由唯一约束兜底。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ChatDevice d where d.username = :username order by d.deviceId asc")
    List<ChatDevice> findByUsernameForUpdate(@Param("username") String username);
}
