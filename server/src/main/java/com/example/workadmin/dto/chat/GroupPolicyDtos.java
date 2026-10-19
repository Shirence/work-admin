package com.example.workadmin.dto.chat;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 建群权限：全局策略 + 账号级覆盖。
 */
public final class GroupPolicyDtos {

    private GroupPolicyDtos() {
    }

    public record AccountPermissionDto(
            String username,
            String overrideMode,
            String updatedBy,
            LocalDateTime updatedAt
    ) {
    }

    public record PolicyDto(
            String mode,
            String updatedBy,
            LocalDateTime updatedAt,
            List<AccountPermissionDto> overrides
    ) {
    }

    public record PolicyUpdateRequest(
            @NotBlank String mode
    ) {
    }

    public record PermissionUpdateRequest(
            @NotBlank String username,
            @NotBlank String overrideMode
    ) {
    }

    public record EffectivePermissionDto(
            String mode,
            String overrideMode,
            boolean canCreateGroup,
            String reason
    ) {
    }
}
