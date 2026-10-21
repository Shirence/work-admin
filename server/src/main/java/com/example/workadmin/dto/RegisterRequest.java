package com.example.workadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（写入 stu 的 `user` 表，与该项目共用账号）。
 * 校验规则与前端保持一致：
 *   账号 4-20 位字母/数字/下划线
 *   密码 6-32 位，必须同时包含字母和数字
 *   姓名 可选，不填则用账号
 */
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "账号需为 4-20 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{6,32}$",
             message = "密码需 6-32 位，且同时包含字母和数字")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String name;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
