package com.example.workadmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 群聊接口集成测试。
 * <p>
 * 数据库换成 H2 内存库，并用 {@code src/test/resources/schema.sql} 造出 stu 的
 * user / sys_* / chat_* 表，所以不会碰到远程 MySQL。
 * 被测的读写逻辑与线上完全一致（同一个 StuStore 的显式 SQL）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:groupchat-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=USER",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupChatIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String adminToken;
    private String viewerToken;

    @BeforeAll
    void login() throws Exception {
        adminToken = login("admin");
        viewerToken = login("viewer");
        assertThat(adminToken).isNotBlank();
        assertThat(viewerToken).isNotBlank();
    }

    @Test
    @Order(1)
    void adminSeesEveryGroup() throws Exception {
        JsonNode body = ok(mvc.perform(get("/api/groups").header("token", adminToken)));
        assertThat(body.size()).isEqualTo(2);
        JsonNode first = body.get(0);
        assertThat(first.get("name").asText()).isNotBlank();
        assertThat(first.get("memberCount").asInt()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void messagesAreReturnedOldestFirst() throws Exception {
        JsonNode body = ok(mvc.perform(get("/api/groups/1/messages").header("token", adminToken)));
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get(0).get("content").asText()).isEqualTo("你好");
        assertThat(body.get(0).get("senderName").asText()).isEqualTo("管理员");
        assertThat(body.get(1).get("content").asText()).isEqualTo("收到");
        assertThat(body.get(0).get("id").asLong()).isLessThan(body.get(1).get("id").asLong());
        // mine 表示「当前登录用户发的」：第一条是 admin 自己发的
        assertThat(body.get(0).get("mine").asBoolean()).isTrue();
        assertThat(body.get(1).get("mine").asBoolean()).isFalse();
    }

    @Test
    @Order(3)
    void sentMessageIsVisibleRightAway() throws Exception {
        JsonNode sent = ok(mvc.perform(post("/api/groups/1/messages")
                .header("token", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"集成测试消息\",\"clientMessageId\":\"it-send-1\"}")));
        assertThat(sent.get("content").asText()).isEqualTo("集成测试消息");
        assertThat(sent.get("mine").asBoolean()).isTrue();
        assertThat(sent.get("sentAt").asText()).isNotBlank();

        JsonNode list = ok(mvc.perform(get("/api/groups/1/messages").header("token", adminToken)));
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(2).get("content").asText()).isEqualTo("集成测试消息");
    }

    @Test
    @Order(4)
    void duplicateClientMessageIdKeepsOneRow() throws Exception {
        String payload = "{\"content\":\"重复提交\",\"clientMessageId\":\"it-dup-1\"}";
        for (int i = 0; i < 2; i++) {
            ok(mvc.perform(post("/api/groups/1/messages")
                    .header("token", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)));
        }
        JsonNode list = ok(mvc.perform(get("/api/groups/1/messages").header("token", adminToken)));
        long dup = 0;
        for (JsonNode row : list) {
            if ("重复提交".equals(row.get("content").asText())) dup++;
        }
        assertThat(dup).isEqualTo(1);
    }

    @Test
    @Order(5)
    void blankContentIsRejected() throws Exception {
        JsonNode body = fail(mvc.perform(post("/api/groups/1/messages")
                .header("token", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"   \"}")));
        assertThat(body.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @Order(6)
    void nonMemberCannotReadUntilJoining() throws Exception {
        JsonNode denied = fail(mvc.perform(get("/api/groups/1/messages").header("token", viewerToken)));
        assertThat(denied.get("code").asInt()).isEqualTo(403);

        JsonNode detail = ok(mvc.perform(post("/api/groups/1/join").header("token", viewerToken)));
        assertThat(detail.get("group").get("joined").asBoolean()).isTrue();
        assertThat(detail.get("members").size()).isGreaterThan(1);

        JsonNode list = ok(mvc.perform(get("/api/groups/1/messages").header("token", viewerToken)));
        assertThat(list.size()).isGreaterThan(0);
    }

    @Test
    @Order(7)
    void newGroupHasOwnerAndInvitedMember() throws Exception {
        JsonNode created = ok(mvc.perform(post("/api/groups")
                .header("token", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"集成测试群\",\"memberIds\":[52]}")));
        JsonNode group = created.get("group");
        assertThat(group.get("name").asText()).isEqualTo("集成测试群");
        assertThat(group.get("memberCount").asInt()).isEqualTo(2);
        assertThat(created.get("members").size()).isEqualTo(2);

        long groupId = group.get("id").asLong();
        JsonNode list = ok(mvc.perform(get("/api/groups").header("token", adminToken)));
        boolean found = false;
        for (JsonNode row : list) {
            if (row.get("id").asLong() == groupId) found = true;
        }
        assertThat(found).isTrue();
    }

    @Test
    @Order(8)
    void unknownGroupReturns404() throws Exception {
        JsonNode body = fail(mvc.perform(get("/api/groups/999999/messages").header("token", adminToken)));
        assertThat(body.get("code").asInt()).isEqualTo(404);
    }

    private String login(String account) throws Exception {
        MvcResult result = mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + account + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return ok(result).get("token").asText();
    }

    private JsonNode ok(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return ok(actions.andExpect(status().isOk()).andReturn());
    }

    private JsonNode ok(MvcResult result) throws Exception {
        JsonNode root = json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(root.get("code").asInt()).as(root.toString()).isEqualTo(200);
        return root.get("data");
    }

    private JsonNode fail(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        MvcResult result = actions.andExpect(status().isOk()).andReturn();
        JsonNode root = json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(root.get("code").asInt()).as(root.toString()).isNotEqualTo(200);
        return root;
    }
}
