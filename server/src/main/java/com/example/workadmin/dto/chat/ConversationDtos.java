package com.example.workadmin.dto.chat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话、成员、已读游标与多端同步状态。
 */
public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record PrivateRequest(
            @NotBlank String targetUsername
    ) {
    }

    public record GroupRequest(
            @NotBlank @Size(max = 80) String name,
            @NotEmpty List<@NotBlank String> memberUsernames
    ) {
    }

    public record AddMembersRequest(
            @NotEmpty List<@NotBlank String> usernames
    ) {
    }

    public record RemoveMemberRequest(
            @NotBlank String username
    ) {
    }

    public record RenameRequest(
            @NotBlank @Size(max = 80) String name
    ) {
    }

    public record ReadCursorRequest(
            @NotBlank String deviceKey,
            @NotNull @Min(0) Long lastReadSeq
    ) {
    }

    public record MemberDto(
            String username,
            String role,
            LocalDateTime joinedAt
    ) {
    }

    public record ConversationDto(
            Long id,
            String type,
            String name,
            String ownerUsername,
            Long version,
            Long lastMessageSeq,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String myRole,
            Long lastReadSeq,
            long unreadCount,
            List<MemberDto> members
    ) {
    }

    public record SyncConversationDto(
            Long id,
            String type,
            String name,
            Long version,
            Long lastMessageSeq,
            Long lastReadSeq,
            long unreadCount,
            LocalDateTime updatedAt
    ) {
    }

    public record SyncStateDto(
            Integer deviceId,
            String deviceKey,
            String deviceName,
            String deviceStatus,
            long pendingDeviceLinks,
            List<SyncConversationDto> conversations
    ) {
    }
}
