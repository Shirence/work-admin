package com.example.workadmin.controller;

import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.dto.MenuDtos;
import com.example.workadmin.service.MenuService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @RequirePermission("menu:read")
    public Result<List<MenuDtos.MenuRow>> list() {
        return Result.ok(menuService.list());
    }

    @GetMapping("/permission-tree")
    @RequirePermission("role:grant")
    public Result<List<MenuDtos.MenuRow>> permissionTree() {
        return Result.ok(menuService.permissionTree());
    }

    @PostMapping
    @RequirePermission("menu:create")
    public Result<MenuDtos.MenuRow> create(@RequestBody MenuDtos.MenuRequest request) {
        return Result.ok("节点已创建", menuService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("menu:update")
    public Result<MenuDtos.MenuRow> update(@PathVariable Long id,
                                           @RequestBody MenuDtos.MenuRequest request) {
        return Result.ok("节点已更新", menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok("节点已删除", null);
    }
}
