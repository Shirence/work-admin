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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限边界的回归测试。
 * <p>
 * 这里补的是「角色变更」这条最容易出事、又最容易被漏测的路径：
 * 一个只有 user:assign-role、不是超管的账号，能不能把超管降级 / 把别人提成超管。
 * 之前的实现判断错了对象（判断被改的人而不是操作者），任何人都能降级超管。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:roleguard-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=USER",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "work-admin.auth.token-store=memory"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleGuardTest {

    /** 三重 MD5 的 123456，和 schema.sql 里的种子账号一致。 */
    private static final String PWD_HASH = "c56d0e9a7ccec67b4ea131655038d604";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String opsToken;
    private String adminToken;

    @BeforeAll
    void setUp() {
        // 造一个「有 user:assign-role、但不是超管」的角色，以及一个只有群成员身份的用户
        jdbc.update("insert into sys_role(id, name, code, description, enabled, builtin) "
                + "values (3, '运维', 'ops', '只有用户管理权限', 1, 0)");
        jdbc.update("insert into sys_menu(id, parent_id, name, type, path, permission, sort_order, enabled, builtin) "
                + "values (50, 10, '绑定角色', 'BUTTON', null, 'user:assign-role', 1, 1, 0)");
        jdbc.update("insert into sys_role_menu(role_id, menu_id) values (3, 10), (3, 50)");
        jdbc.update("insert into user(id, name, account, passwd) values (60, '运维', 'ops', ?)", PWD_HASH);
        jdbc.update("insert into sys_user_role(user_id, role_id) values (60, 3)");
        jdbc.update("insert into user(id, name, account, passwd) values (61, '路人', 'guest', ?)", PWD_HASH);
        jdbc.update("insert into sys_user_role(user_id, role_id) values (61, 2)");
        jdbc.update("insert into chat_member(group_id, user_id) values (1, 61)");

        opsToken = login("ops");
        adminToken = login("admin");
    }

    @Test
    @Order(1)
    void nonSuperAdminCannotDemoteSuperAdmin() throws Exception {
        JsonNode body = fail(mvc.perform(put("/api/users/50/role")
                .header("token", opsToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleId\":2}")));
        assertThat(body.get("code").asInt()).isEqualTo(403);
        // 必须是「超管保护」这条规则拦下的，而不是碰巧没有 user:assign-role 权限
        assertThat(body.get("msg").asText()).contains("super_admin");
        assertThat(roleIdOf(50L)).isEqualTo(1L);
    }

    @Test
    @Order(2)
    void nonSuperAdminCannotGrantSuperAdmin() throws Exception {
        JsonNode body = fail(mvc.perform(put("/api/users/60/role")
                .header("token", opsToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleId\":1}")));
        assertThat(body.get("code").asInt()).isEqualTo(403);
        assertThat(body.get("msg").asText()).contains("super_admin");
        assertThat(roleIdOf(60L)).isEqualTo(3L);
    }

    @Test
    @Order(3)
    void superAdminCanStillBindRole() throws Exception {
        ok(mvc.perform(put("/api/users/52/role")
                .header("token", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleId\":2}")));
        assertThat(roleIdOf(52L)).isEqualTo(2L);
    }

    @Test
    @Order(4)
    void deletingUserWhoOnlyHasMemberRowsIsBlocked() throws Exception {
        JsonNode body = fail(mvc.perform(delete("/api/users/61").header("token", adminToken)));
        assertThat(body.get("code").asInt()).isEqualTo(409);
        assertThat(body.get("msg").asText()).contains("成员");
        assertThat(jdbc.queryForObject("select count(*) from user where id = 61", Integer.class)).isEqualTo(1);
    }

    // ---------- 辅助 ----------

    private long roleIdOf(long userId) {
        Long id = jdbc.queryForObject("select role_id from sys_user_role where user_id = ?", Long.class, userId);
        return id == null ? -1 : id;
    }

    private String login(String account) {
        try {
            JsonNode root = json.readTree(mvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + account + "\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            return root.get("data").get("token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("登录失败：" + account, e);
        }
    }

    private JsonNode ok(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        String body = actions.andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = json.readTree(body);
        assertThat(root.get("code").asInt()).as(body).isEqualTo(200);
        return root.get("data");
    }

    private JsonNode fail(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        String body = actions.andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = json.readTree(body);
        assertThat(root.get("code").asInt()).as(body).isNotEqualTo(200);
        return root;
    }
}
