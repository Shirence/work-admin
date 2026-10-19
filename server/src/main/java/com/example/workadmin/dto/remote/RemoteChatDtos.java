package com.example.workadmin.dto.remote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 远程 stu 账号登录与远程群聊的请求/响应。
 */
public final class RemoteChatDtos {

    private RemoteChatDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 100) String account,
            @NotBlank @Size(max = 100) String password
    ) {
    }

    public record RegisterRequest(
            @Size(max = 100) String name,
            @NotBlank @Size(min = 2, max = 100) String account,
            @NotBlank @Size(min = 6, max = 32) String password,
            @NotBlank String confirmPassword
    ) {
    }

    public record GroupRow(
            long id,
            String name,
            Integer ownerId,
            String ownerName,
            String ownerAccount,
            LocalDateTime createdAt,
            long memberCount,
            boolean joined
    ) {
    }

    public record MemberRow(
            long groupId,
            Integer userId,
            LocalDateTime joinedAt,
            long joinedAfterId,
            long historyStartId
    ) {
    }

    public record MessageRow(
            long id,
            long groupId,
            Integer senderId,
            String senderName,
            String content,
            String clientMessageId,
            LocalDateTime sentAt
    ) {
    }

    public record MessagePage(
            List<MessageRow> items,
            long nextAfterId,
            boolean hasMore
    ) {
    }

    public record SendRequest(
            @NotBlank @Size(max = 2000) String content,
            @NotBlank
            @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}", message = "clientMessageId 必须是 1-64 位字母、数字、下划线或短横线")
            String clientMessageId
    ) {
    }

    public record CreateGroupRequest(
            @NotBlank @Size(max = 100) String name
    ) {
    }
}
