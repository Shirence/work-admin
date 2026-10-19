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
 * 消息发送与同步。服务端只处理密文信封，不包含任何明文正文字段。
 */
public final class MessageDtos {

    private MessageDtos() {
    }

    public record EnvelopeRequest(
            @NotBlank String targetDeviceKey,
            @NotNull @Min(1) @Max(3) Integer cipherType,
            @NotBlank String body
    ) {
    }

    public record SendRequest(
            @NotNull Long conversationId,
            @NotBlank @Size(max = 80) String clientMessageId,
            @NotBlank String senderDeviceKey,
            @Size(max = 20) String contentType,
            Long expectedVersion,
            @NotNull @Valid List<EnvelopeRequest> envelopes
    ) {
    }

    public record EnvelopeDto(
            String targetDeviceKey,
            Integer cipherType,
            String body,
            Integer registrationId
    ) {
    }

    public record MessageDto(
            Long id,
            Long conversationId,
            Long serverSeq,
            String senderUsername,
            Integer senderDeviceId,
            String senderDeviceKey,
            String clientMessageId,
            String contentType,
            LocalDateTime createdAt,
            int envelopeCount,
            EnvelopeDto envelope
    ) {
    }

    public record SendResult(
            boolean duplicate,
            MessageDto message,
            Long conversationVersion
    ) {
    }
}
