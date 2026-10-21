package com.example.workadmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 覆盖聊天后端关键链路：
 * 设备注册/新设备授权、私聊、幂等重发、逐设备密文分发、增量同步、
 * 多端已读游标、建群权限、群成员版本号、管理员设备重置。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String managerToken;
    private String viewerToken;

    private JsonNode adminDevice1;
    private JsonNode managerDevice1;
    private JsonNode viewerDevice1;
    private JsonNode adminDevice2;

    private long conversationId;
    private long groupId;

    @Test
    @Order(1)
    void loginSeededAccounts() throws Exception {
        adminToken = login("admin");
        managerToken = login("manager");
        viewerToken = login("viewer");
        assertThat(adminToken).hasSize(32);
        assertThat(managerToken).hasSize(32);
        assertThat(viewerToken).hasSize(32);
    }

    @Test
    @Order(2)
    void firstDevicePerAccountIsActive() throws Exception {
        adminDevice1 = registerDevice(adminToken, "admin-web-1", "Admin Web", 1001, 0x11);
        managerDevice1 = registerDevice(managerToken, "manager-web-1", "Manager Web", 2001, 0x21);
        viewerDevice1 = registerDevice(viewerToken, "viewer-web-1", "Viewer Web", 3001, 0x31);

        assertThat(adminDevice1.path("device").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(adminDevice1.path("device").path("deviceId").asInt()).isEqualTo(1);
        assertThat(adminDevice1.path("pendingApproval").asBoolean()).isFalse();
        assertThat(managerDevice1.path("device").path("deviceId").asInt()).isEqualTo(1);
        assertThat(viewerDevice1.path("device").path("deviceId").asInt()).isEqualTo(1);
    }

    @Test
    @Order(3)
    void keyBundleConsumesOneTimePreKey() throws Exception {
        JsonNode first = getJson("/api/chat/users/manager/device-bundles", adminToken);
        assertThat(first.path("code").asInt()).isEqualTo(200);
        JsonNode bundle = first.path("data").get(0);
        assertThat(bundle.path("clientDeviceKey").asText()).isEqualTo("manager-web-1");
        assertThat(bundle.path("oneTimePreKey").path("keyId").asInt()).isEqualTo(1);

        JsonNode second = getJson("/api/chat/users/manager/device-bundles", adminToken);
        JsonNode consumed = second.path("data").get(0);
        assertThat(consumed.path("oneTimePreKey").isNull()).isTrue();
    }

    @Test
    @Order(4)
    void privateConversationIdempotentSendAndPerDeviceSync() throws Exception {
        JsonNode created = postJson("/api/chat/conversations/private", adminToken,
                Map.of("targetUsername", "manager"));
        assertThat(created.path("code").asInt()).isEqualTo(200);
        conversationId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("type").asText()).isEqualTo("PRIVATE");
        assertThat(created.path("data").path("members").size()).isEqualTo(2);

        String body = cipher("hello-manager");
        JsonNode sent = send(adminToken, conversationId, "admin-web-1", "admin-cmsg-1",
                Map.of("targetDeviceKey", "manager-web-1", "cipherType", 3, "body", body));
        assertThat(sent.path("code").asInt()).isEqualTo(200);
        assertThat(sent.path("data").path("duplicate").asBoolean()).isFalse();
        assertThat(sent.path("data").path("message").path("serverSeq").asLong()).isEqualTo(1);
        assertThat(sent.path("data").path("message").path("envelopeCount").asInt()).isEqualTo(1);

        JsonNode duplicate = send(adminToken, conversationId, "admin-web-1", "admin-cmsg-1",
                Map.of("targetDeviceKey", "manager-web-1", "cipherType", 3, "body", body));
        assertThat(duplicate.path("data").path("duplicate").asBoolean()).isTrue();
        assertThat(duplicate.path("data").path("message").path("serverSeq").asLong()).isEqualTo(1);

        JsonNode managerSync = getJson("/api/chat/messages?conversationId=" + conversationId
                + "&deviceKey=manager-web-1&afterSeq=0", managerToken);
        assertThat(managerSync.path("data").size()).isEqualTo(1);
        JsonNode message = managerSync.path("data").get(0);
        assertThat(message.path("serverSeq").asLong()).isEqualTo(1);
        assertThat(message.path("envelope").path("targetDeviceKey").asText()).isEqualTo("manager-web-1");
        assertThat(message.path("envelope").path("body").asText()).isEqualTo(body);

        JsonNode senderSync = getJson("/api/chat/messages?conversationId=" + conversationId
                + "&deviceKey=admin-web-1&afterSeq=0", adminToken);
        assertThat(senderSync.path("data").get(0).path("envelope").isNull()).isTrue();
    }

    @Test
    @Order(5)
    void newDeviceRequiresApprovalAndReceivesHistoryGrant() throws Exception {
        adminDevice2 = registerDevice(adminToken, "admin-web-2", "Admin Laptop", 1002, 0x41);
        assertThat(adminDevice2.path("device").path("status").asText()).isEqualTo("PENDING");
        assertThat(adminDevice2.path("pendingApproval").asBoolean()).isTrue();
        assertThat(adminDevice2.path("device").path("deviceId").asInt()).isEqualTo(2);

        JsonNode denied = getJson("/api/chat/sync/state?deviceKey=admin-web-2", adminToken);
        assertThat(denied.path("code").asInt()).isEqualTo(403);

        JsonNode pending = getJson("/api/chat/devices/pending?deviceKey=admin-web-1", adminToken);
        assertThat(pending.path("data").size()).isEqualTo(1);
        assertThat(pending.path("data").get(0).path("newDeviceKey").asText()).isEqualTo("admin-web-2");

        JsonNode approved = postJson("/api/chat/devices/approve", adminToken, Map.of(
                "approverDeviceKey", "admin-web-1",
                "targetDeviceKey", "admin-web-2",
                "grants", List.of(Map.of("cipherType", 2, "body", cipher("history-key-for-new-device")))
        ));
        assertThat(approved.path("code").asInt()).isEqualTo(200);
        assertThat(approved.path("data").path("status").asText()).isEqualTo("ACTIVE");

        JsonNode grants = getJson("/api/chat/devices/grants?deviceKey=admin-web-2", adminToken);
        assertThat(grants.path("data").size()).isEqualTo(1);
        assertThat(grants.path("data").get(0).path("fromDeviceKey").asText()).isEqualTo("admin-web-1");

        JsonNode state = getJson("/api/chat/sync/state?deviceKey=admin-web-2", adminToken);
        assertThat(state.path("code").asInt()).isEqualTo(200);
        assertThat(state.path("data").path("deviceStatus").asText()).isEqualTo("ACTIVE");
        assertThat(state.path("data").path("pendingDeviceLinks").asLong()).isZero();
    }

    @Test
    @Order(6)
    void messageIsEncryptedSeparatelyForEveryActiveDevice() throws Exception {
        JsonNode sent = send(managerToken, conversationId, "manager-web-1", "manager-cmsg-1",
                Map.of("targetDeviceKey", "admin-web-1", "cipherType", 3, "body", cipher("for-admin-web-1")),
                Map.of("targetDeviceKey", "admin-web-2", "cipherType", 2, "body", cipher("for-admin-web-2")));
        assertThat(sent.path("code").asInt()).isEqualTo(200);
        assertThat(sent.path("data").path("message").path("serverSeq").asLong()).isEqualTo(2);
        assertThat(sent.path("data").path("message").path("envelopeCount").asInt()).isEqualTo(2);

        JsonNode admin2Sync = getJson("/api/chat/messages?conversationId=" + conversationId
                + "&deviceKey=admin-web-2&afterSeq=0", adminToken);
        assertThat(admin2Sync.path("data").size()).isEqualTo(2);
        assertThat(admin2Sync.path("data").get(0).path("serverSeq").asLong()).isEqualTo(1);
        assertThat(admin2Sync.path("data").get(0).path("envelope").isNull()).isTrue();
        assertThat(admin2Sync.path("data").get(1).path("envelope").path("targetDeviceKey").asText())
                .isEqualTo("admin-web-2");
        assertThat(admin2Sync.path("data").get(1).path("envelope").path("body").asText())
                .isEqualTo(cipher("for-admin-web-2"));

        JsonNode admin1Sync = getJson("/api/chat/messages?conversationId=" + conversationId
                + "&deviceKey=admin-web-1&afterSeq=0", adminToken);
        assertThat(admin1Sync.path("data").get(1).path("envelope").path("targetDeviceKey").asText())
                .isEqualTo("admin-web-1");
        assertThat(admin1Sync.path("data").get(1).path("envelope").path("body").asText())
                .isEqualTo(cipher("for-admin-web-1"));

        JsonNode managerSync = getJson("/api/chat/messages?conversationId=" + conversationId
                + "&deviceKey=manager-web-1&afterSeq=0", managerToken);
        assertThat(managerSync.path("data").get(0).path("envelope").path("body").asText())
                .isEqualTo(cipher("hello-manager"));
        assertThat(managerSync.path("data").get(1).path("envelope").isNull()).isTrue();
    }

    @Test
    @Order(7)
    void readCursorIsPerDevice() throws Exception {
        JsonNode managerRead = postJson("/api/chat/conversations/" + conversationId + "/read", managerToken,
                Map.of("deviceKey", "manager-web-1", "lastReadSeq", 2));
        assertThat(managerRead.path("code").asInt()).isEqualTo(200);
        assertThat(managerRead.path("data").path("unreadCount").asLong()).isZero();

        JsonNode admin2Read = postJson("/api/chat/conversations/" + conversationId + "/read", adminToken,
                Map.of("deviceKey", "admin-web-2", "lastReadSeq", 1));
        assertThat(admin2Read.path("data").path("lastReadSeq").asLong()).isEqualTo(1);
        assertThat(admin2Read.path("data").path("unreadCount").asLong()).isEqualTo(1);

        JsonNode state = getJson("/api/chat/sync/state?deviceKey=manager-web-1", managerToken);
        JsonNode syncConversation = state.path("data").path("conversations").get(0);
        assertThat(syncConversation.path("lastReadSeq").asLong()).isEqualTo(2);
        assertThat(syncConversation.path("unreadCount").asLong()).isZero();
    }

    @Test
    @Order(8)
    void groupCreationFollowsGlobalAndAccountPermissions() throws Exception {
        JsonNode created = postJson("/api/chat/conversations/group", managerToken, Map.of(
                "name", "项目组",
                "memberUsernames", List.of("admin", "viewer")));
        assertThat(created.path("code").asInt()).isEqualTo(200);
        groupId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("version").asLong()).isEqualTo(1);
        assertThat(created.path("data").path("members").size()).isEqualTo(3);
        assertThat(created.path("data").path("myRole").asText()).isEqualTo("OWNER");

        JsonNode deniedByPolicy = postJson("/api/chat/conversations/group", managerToken, Map.of(
                "name", "应被拒绝", "memberUsernames", List.of("admin")));
        assertThat(deniedByPolicy.path("code").asInt()).isEqualTo(200); // 先建成功（AUTO_ALLOW）
        assertThat(deniedByPolicy.path("data").path("id").asLong()).isPositive();

        JsonNode policy = postJson("/api/chat/admin/group-policy", adminToken, Map.of("mode", "AUTO_DENY"));
        assertThat(policy.path("data").path("mode").asText()).isEqualTo("AUTO_DENY");
        assertThat(postJson("/api/chat/conversations/group", managerToken, Map.of(
                "name", "被全局拒绝", "memberUsernames", List.of("admin")))
                .path("code").asInt()).isEqualTo(403);

        JsonNode allow = postJson("/api/chat/admin/account-permissions", adminToken, Map.of(
                "username", "manager", "overrideMode", "ALLOW"));
        assertThat(allow.path("data").path("overrideMode").asText()).isEqualTo("ALLOW");
        assertThat(postJson("/api/chat/conversations/group", managerToken, Map.of(
                "name", "账号单独允许", "memberUsernames", List.of("admin")))
                .path("code").asInt()).isEqualTo(200);

        postJson("/api/chat/admin/account-permissions", adminToken, Map.of(
                "username", "manager", "overrideMode", "DENY"));
        assertThat(postJson("/api/chat/conversations/group", managerToken, Map.of(
                "name", "账号被拒绝", "memberUsernames", List.of("admin")))
                .path("code").asInt()).isEqualTo(403);

        postJson("/api/chat/admin/account-permissions", adminToken, Map.of(
                "username", "manager", "overrideMode", "INHERIT"));
        postJson("/api/chat/admin/group-policy", adminToken, Map.of("mode", "AUTO_ALLOW"));

        JsonNode effective = getJson("/api/chat/group-permission", managerToken);
        assertThat(effective.path("data").path("canCreateGroup").asBoolean()).isTrue();
    }

    @Test
    @Order(9)
    void groupMembershipAndRenameBumpVersion() throws Exception {
        JsonNode renamed = postJson("/api/chat/conversations/" + groupId + "/rename", managerToken,
                Map.of("name", "项目组-改名"));
        assertThat(renamed.path("data").path("version").asLong()).isEqualTo(2);

        JsonNode removed = postJson("/api/chat/conversations/" + groupId + "/members/remove", managerToken,
                Map.of("username", "viewer"));
        assertThat(removed.path("data").path("version").asLong()).isEqualTo(3);
        assertThat(removed.path("data").path("members").size()).isEqualTo(2);

        JsonNode added = postJson("/api/chat/conversations/" + groupId + "/members", managerToken,
                Map.of("usernames", List.of("viewer")));
        assertThat(added.path("data").path("version").asLong()).isEqualTo(4);
        assertThat(added.path("data").path("members").size()).isEqualTo(3);
    }

    @Test
    @Order(10)
    void groupMessageFansOutToAllActiveDevices() throws Exception {
        JsonNode sent = send(managerToken, groupId, "manager-web-1", "group-cmsg-1",
                Map.of("targetDeviceKey", "admin-web-1", "cipherType", 3, "body", cipher("group-admin-1")),
                Map.of("targetDeviceKey", "admin-web-2", "cipherType", 2, "body", cipher("group-admin-2")),
                Map.of("targetDeviceKey", "viewer-web-1", "cipherType", 3, "body", cipher("group-viewer")));
        assertThat(sent.path("code").asInt()).isEqualTo(200);
        assertThat(sent.path("data").path("message").path("serverSeq").asLong()).isEqualTo(1);
        assertThat(sent.path("data").path("message").path("envelopeCount").asInt()).isEqualTo(3);

        JsonNode viewerSync = getJson("/api/chat/messages?conversationId=" + groupId
                + "&deviceKey=viewer-web-1&afterSeq=0", viewerToken);
        assertThat(viewerSync.path("data").size()).isEqualTo(1);
        assertThat(viewerSync.path("data").get(0).path("envelope").path("body").asText())
                .isEqualTo(cipher("group-viewer"));
    }

    @Test
    @Order(11)
    void adminCanResetLostDevices() throws Exception {
        JsonNode devices = getJson("/api/chat/admin/devices?username=viewer", adminToken);
        assertThat(devices.path("data").size()).isEqualTo(1);

        JsonNode reset = postJson("/api/chat/admin/devices/reset", adminToken, Map.of("username", "viewer"));
        assertThat(reset.path("code").asInt()).isEqualTo(200);
        assertThat(getJson("/api/chat/admin/devices?username=viewer", adminToken).path("data").size()).isZero();

        JsonNode fresh = registerDevice(viewerToken, "viewer-web-2", "Viewer New Web", 3002, 0x51);
        assertThat(fresh.path("device").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(fresh.path("device").path("deviceId").asInt()).isEqualTo(1);
    }

    @Test
    @Order(12)
    void nonAdminCannotChangeGroupPolicy() throws Exception {
        JsonNode denied = getJson("/api/chat/admin/group-policy", managerToken);
        assertThat(denied.path("code").asInt()).isEqualTo(403);
    }

    private String login(String username) throws Exception {
        JsonNode result = postJson("/api/login", null,
                Map.of("username", username, "password", "123456"));
        assertThat(result.path("code").asInt()).isEqualTo(200);
        return result.path("data").path("token").asText();
    }

    private JsonNode registerDevice(String token,
                                    String clientDeviceKey,
                                    String deviceName,
                                    int registrationId,
                                    int seed) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientDeviceKey", clientDeviceKey);
        body.put("deviceName", deviceName);
        body.put("registrationId", registrationId);
        body.put("identityKey", key(seed));
        body.put("signedPreKeyId", 1);
        body.put("signedPreKeyPublic", key(seed + 1));
        body.put("signedPreKeySignature", signature(seed + 2));
        body.put("oneTimePreKeys", List.of(Map.of("keyId", 1, "publicKey", key(seed + 3))));
        JsonNode result = postJson("/api/chat/devices/register", token, body);
        assertThat(result.path("code").asInt()).isEqualTo(200);
        return result.path("data");
    }

    private JsonNode send(String token,
                          long conversationId,
                          String senderDeviceKey,
                          String clientMessageId,
                          Map<String, Object>... envelopes) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conversationId", conversationId);
        body.put("clientMessageId", clientMessageId);
        body.put("senderDeviceKey", senderDeviceKey);
        body.put("contentType", "TEXT");
        body.put("envelopes", new ArrayList<>(Arrays.asList(envelopes)));
        return postJson("/api/chat/messages", token, body);
    }

    private JsonNode postJson(String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder builder = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body));
        if (token != null) {
            builder.header("token", token);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MockHttpServletRequestBuilder builder = get(path);
        if (token != null) {
            builder.header("token", token);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String cipher(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String key(int seed) {
        byte[] bytes = new byte[33];
        bytes[0] = 0x05;
        Arrays.fill(bytes, 1, bytes.length, (byte) seed);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String signature(int seed) {
        byte[] bytes = new byte[64];
        Arrays.fill(bytes, (byte) seed);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
