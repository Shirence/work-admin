package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.ChatConstants;
import com.example.workadmin.dto.chat.GroupPolicyDtos;
import com.example.workadmin.entity.ChatAccountGroupPermission;
import com.example.workadmin.entity.ChatGroupPolicy;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.ChatAccountGroupPermissionRepository;
import com.example.workadmin.repository.ChatGroupPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 建群权限：
 * <pre>
 * 账号覆盖 = ALLOW  -> 允许
 * 账号覆盖 = DENY   -> 拒绝
 * 账号覆盖 = INHERIT
 *   ├─ 全局 AUTO_ALLOW -> 允许
 *   ├─ 全局 AUTO_DENY  -> 拒绝
 *   └─ 全局 MANUAL     -> 拒绝（需管理员把账号覆盖设为 ALLOW）
 * </pre>
 */
@Service
public class ChatGroupPolicyService {

    private static final Set<String> MODES = Set.of(
            ChatConstants.POLICY_AUTO_ALLOW,
            ChatConstants.POLICY_AUTO_DENY,
            ChatConstants.POLICY_MANUAL
    );

    private static final Set<String> OVERRIDES = Set.of(
            ChatConstants.OVERRIDE_INHERIT,
            ChatConstants.OVERRIDE_ALLOW,
            ChatConstants.OVERRIDE_DENY
    );

    private final ChatGroupPolicyRepository policyRepository;
    private final ChatAccountGroupPermissionRepository permissionRepository;
    private final AccountRepository accountRepository;

    public ChatGroupPolicyService(ChatGroupPolicyRepository policyRepository,
                                  ChatAccountGroupPermissionRepository permissionRepository,
                                  AccountRepository accountRepository) {
        this.policyRepository = policyRepository;
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public ChatGroupPolicy getOrCreatePolicy() {
        return policyRepository.findById(1L).orElseGet(() -> {
            ChatGroupPolicy policy = new ChatGroupPolicy();
            policy.setMode(ChatConstants.POLICY_AUTO_ALLOW);
            policy.setUpdatedBy("system");
            policy.setUpdatedAt(LocalDateTime.now());
            return policyRepository.save(policy);
        });
    }

    @Transactional
    public GroupPolicyDtos.PolicyDto getPolicy() {
        ChatGroupPolicy policy = getOrCreatePolicy();
        List<GroupPolicyDtos.AccountPermissionDto> overrides = permissionRepository
                .findAllByOrderByUsernameAsc()
                .stream()
                .map(this::toDto)
                .toList();
        return new GroupPolicyDtos.PolicyDto(
                policy.getMode(),
                policy.getUpdatedBy(),
                policy.getUpdatedAt(),
                overrides
        );
    }

    @Transactional
    public GroupPolicyDtos.PolicyDto updatePolicy(String mode, String adminUsername) {
        String normalized = normalizeMode(mode);
        ChatGroupPolicy policy = getOrCreatePolicy();
        policy.setMode(normalized);
        policy.setUpdatedBy(adminUsername);
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
        return getPolicy();
    }

    @Transactional
    public GroupPolicyDtos.AccountPermissionDto setOverride(String adminUsername,
                                                             String targetUsername,
                                                             String overrideMode) {
        String username = targetUsername == null ? "" : targetUsername.trim();
        if (!accountRepository.existsByUsername(username)) {
            throw new BizException(404, "账号不存在：" + username);
        }
        String normalized = normalizeOverride(overrideMode);
        ChatAccountGroupPermission permission = permissionRepository.findByUsername(username)
                .orElseGet(() -> {
                    ChatAccountGroupPermission p = new ChatAccountGroupPermission();
                    p.setUsername(username);
                    return p;
                });
        permission.setOverrideMode(normalized);
        permission.setUpdatedBy(adminUsername);
        permission.setUpdatedAt(LocalDateTime.now());
        return toDto(permissionRepository.save(permission));
    }

    @Transactional
    public GroupPolicyDtos.EffectivePermissionDto effective(String username) {
        ChatGroupPolicy policy = getOrCreatePolicy();
        String override = permissionRepository.findByUsername(username)
                .map(ChatAccountGroupPermission::getOverrideMode)
                .orElse(ChatConstants.OVERRIDE_INHERIT);

        boolean allowed;
        String reason;
        if (ChatConstants.OVERRIDE_ALLOW.equals(override)) {
            allowed = true;
            reason = "账号已被管理员单独允许建群";
        } else if (ChatConstants.OVERRIDE_DENY.equals(override)) {
            allowed = false;
            reason = "账号已被管理员禁止建群";
        } else {
            switch (policy.getMode()) {
                case ChatConstants.POLICY_AUTO_ALLOW -> {
                    allowed = true;
                    reason = "全局策略允许建群";
                }
                case ChatConstants.POLICY_AUTO_DENY -> {
                    allowed = false;
                    reason = "全局策略已关闭建群";
                }
                default -> {
                    allowed = false;
                    reason = "当前为手动审批模式，请联系管理员开通建群权限";
                }
            }
        }
        return new GroupPolicyDtos.EffectivePermissionDto(policy.getMode(), override, allowed, reason);
    }

    @Transactional
    public void assertCanCreateGroup(String username) {
        GroupPolicyDtos.EffectivePermissionDto effective = effective(username);
        if (!effective.canCreateGroup()) {
            throw new BizException(403, effective.reason());
        }
    }

    private GroupPolicyDtos.AccountPermissionDto toDto(ChatAccountGroupPermission p) {
        return new GroupPolicyDtos.AccountPermissionDto(
                p.getUsername(), p.getOverrideMode(), p.getUpdatedBy(), p.getUpdatedAt());
    }

    private String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase();
        if (!MODES.contains(normalized)) {
            throw new BizException(400, "mode 必须是 AUTO_ALLOW / AUTO_DENY / MANUAL");
        }
        return normalized;
    }

    private String normalizeOverride(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase();
        if (!OVERRIDES.contains(normalized)) {
            throw new BizException(400, "overrideMode 必须是 INHERIT / ALLOW / DENY");
        }
        return normalized;
    }
}
