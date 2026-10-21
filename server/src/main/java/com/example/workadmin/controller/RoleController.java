package com.example.workadmin.controller;

import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.dto.RoleDtos;
import com.example.workadmin.service.RoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @RequirePermission("role:read")
    public Result<List<RoleDtos.RoleRow>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/options")
    @RequirePermission("user:assign-role")
    public Result<List<RoleDtos.RoleOption>> options() {
        return Result.ok(roleService.options());
    }

    @PostMapping
    @RequirePermission("role:create")
    public Result<RoleDtos.RoleRow> create(@RequestBody RoleDtos.RoleRequest request) {
        return Result.ok("角色已创建", roleService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("role:update")
    public Result<RoleDtos.RoleRow> update(@PathVariable Long id,
                                           @RequestBody RoleDtos.RoleRequest request) {
        return Result.ok("角色已更新", roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok("角色已删除", null);
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission("role:grant")
    public Result<List<Long>> permissions(@PathVariable Long id) {
        return Result.ok(roleService.permissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission("role:grant")
    public Result<Void> savePermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.savePermissions(id, body == null ? List.of() : body.get("menuIds"));
        return Result.ok("授权已保存", null);
    }
}
