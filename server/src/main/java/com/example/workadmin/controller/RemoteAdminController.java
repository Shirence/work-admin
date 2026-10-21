package com.example.workadmin.controller;

import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.dto.remote.RemoteAdminDtos;
import com.example.workadmin.service.RemoteAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/remote-admin")
public class RemoteAdminController {

    private final RemoteAdminService remoteAdminService;

    public RemoteAdminController(RemoteAdminService remoteAdminService) {
        this.remoteAdminService = remoteAdminService;
    }

    @GetMapping("/users")
    @RequirePermission("remote:user:read")
    public Result<List<RemoteAdminDtos.RemoteUserRow>> users(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(remoteAdminService.users(keyword));
    }

    @GetMapping("/roles")
    @RequirePermission("remote:user:read")
    public Result<List<RemoteAdminDtos.RemoteRoleRow>> roles() {
        return Result.ok(remoteAdminService.roles());
    }

    @PostMapping("/users")
    @RequirePermission("remote:user:create")
    public Result<RemoteAdminDtos.RemoteUserRow> create(@RequestBody RemoteAdminDtos.RemoteUserRequest request) {
        return Result.ok("远程用户已创建", remoteAdminService.create(request));
    }

    @PutMapping("/users/{id}")
    @RequirePermission("remote:user:update")
    public Result<RemoteAdminDtos.RemoteUserRow> update(@PathVariable int id,
                                                        @RequestBody RemoteAdminDtos.RemoteUserRequest request) {
        return Result.ok("远程用户已更新", remoteAdminService.update(id, request));
    }

    @PutMapping("/users/{id}/role")
    @RequirePermission("remote:user:assign-role")
    public Result<RemoteAdminDtos.RemoteUserRow> bindRole(@PathVariable int id,
                                                          @RequestBody RemoteAdminDtos.RoleBindRequest request) {
        if (request == null || request.roleId() == null) {
            return Result.fail(400, "请选择远程角色");
        }
        return Result.ok("远程角色已更新", remoteAdminService.bindRole(id, request.roleId()));
    }

    @DeleteMapping("/users/{id}")
    @RequirePermission("remote:user:delete")
    public Result<Void> delete(@PathVariable int id) {
        remoteAdminService.delete(id);
        return Result.ok("远程用户已删除", null);
    }
}
