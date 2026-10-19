package com.example.workadmin.dto.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备注册、授权、密钥包相关的请求/响应。
 */
public final class DeviceDtos {

    private DeviceDtos() {
    }

    public record OneTimePreKeyDto(
            @NotNull @Min(1) Integer keyId,
            @NotBlank String publicKey
    ) {
    }

    public record DeviceRegisterRequest(
            @NotBlank @Size(max = 80) String clientDeviceKey,
            @Size(max = 120) String deviceName,
            @NotNull @Min(1) @Max(16383) Integer registrationId,
            @NotBlank String identityKey,
            @NotNull @Min(1) Integer signedPreKeyId,
            @NotBlank String signedPreKeyPublic,
            @NotBlank String signedPreKeySignature,
            @Valid List<OneTimePreKeyDto> oneTimePreKeys
    ) {
    }

    public record DeviceDto(
            Long id,
            Integer deviceId,
            String clientDeviceKey,
            String deviceName,
            String status,
            Integer registrationId,
            String approvedByDeviceKey,
            LocalDateTime approvedAt,
            LocalDateTime createdAt,
            LocalDateTime lastSeenAt
    ) {
    }

    public record RegisterResult(
            DeviceDto device,
            boolean pendingApproval,
            Long linkId
    ) {
    }

    public record DeviceLinkDto(
            Long id,
            Integer newDeviceId,
            String newDeviceKey,
            String newDeviceName,
            String status,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt
    ) {
    }

    public record GrantPayload(
            @NotNull @Min(1) @Max(3) Integer cipherType,
            @NotBlank String body
    ) {
    }

    public record ApproveRequest(
            @NotBlank String approverDeviceKey,
            @NotBlank String targetDeviceKey,
            @Valid List<GrantPayload> grants
    ) {
    }

    public record RejectRequest(
            @NotBlank String approverDeviceKey,
            @NotBlank String targetDeviceKey
    ) {
    }

    public record RevokeRequest(
            @NotBlank String actorDeviceKey,
            @NotBlank String targetDeviceKey
    ) {
    }

    public record HeartbeatRequest(
            @NotBlank String deviceKey
    ) {
    }

    public record AdminResetRequest(
            @NotBlank String username
    ) {
    }

    public record GrantDto(
            Long id,
            Long linkId,
            String fromDeviceKey,
            Integer cipherType,
            String body,
            LocalDateTime createdAt
    ) {
    }

    public record OneTimePreKeyPublicDto(
            Integer keyId,
            String publicKey
    ) {
    }

    public record DeviceBundleDto(
            String username,
            Integer deviceId,
            String clientDeviceKey,
            Integer registrationId,
            String identityKey,
            Integer signedPreKeyId,
            String signedPreKeyPublic,
            String signedPreKeySignature,
            OneTimePreKeyPublicDto oneTimePreKey
    ) {
    }
}
