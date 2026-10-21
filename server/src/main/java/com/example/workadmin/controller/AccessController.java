package com.example.workadmin.controller;

import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.AccessDtos;
import com.example.workadmin.dto.AccountDtos;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.service.AccessService;
import com.example.workadmin.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AccessController {

    private final AccessService accessService;
    private final RoleService roleService;

    public AccessController(AccessService accessService, RoleService roleService) {
        this.accessService = accessService;
        this.roleService = roleService;
    }

    @GetMapping("/auth/access")
    public Result<AccessDtos.AccessResponse> access(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(accessService.access(userInfo.getUsername()));
    }

    @GetMapping("/users")
    @RequirePermission("user:read")
    public Result<List<AccountDtos.AccountRow>> accounts() {
        return Result.ok(roleService.accounts());
    }

    @PutMapping("/users/{id}/role")
    @RequirePermission("user:assign-role")
    public Result<AccountDtos.AccountRow> bindRole(@PathVariable Long id,
                                                    @RequestBody AccountDtos.RoleBindRequest request) {
        if (request == null || request.roleId() == null) {
            return Result.fail(400, "请选择角色");
        }
        return Result.ok("角色已更新", roleService.bindRole(id, request.roleId()));
    }
}
