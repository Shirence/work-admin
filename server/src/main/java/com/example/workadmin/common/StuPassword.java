package com.example.workadmin.common;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 与 stu 项目（com.example.demo.system.auth.PasswordCodec）完全一致的密码编码。
 * <p>
 * 现役规则：md5(md5(md5(明文)))；历史数据可能是单次 md5，登录时做兼容校验。
 * 中台与 stu 共用同一张 user 表，密码算法必须保持一致，否则两边无法互相登录。
 */
public final class StuPassword {

    private StuPassword() {
    }

    /** 写入新密码时使用：三重 MD5。 */
    public static String hash(String raw) {
        return md5(md5(md5(raw)));
    }

    /** 登录校验：先按三重 MD5，再兼容旧数据的单次 MD5。 */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null || stored.isBlank()) return false;
        String normalized = stored.trim().toLowerCase();
        return md5(md5(md5(raw))).equals(normalized) || md5(raw).equals(normalized);
    }

    private static String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}
