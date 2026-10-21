package com.example.workadmin.controller;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.dto.UserRequest;
import com.example.workadmin.entity.User;
import com.example.workadmin.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/user/list?keyword=张 */
    @GetMapping("/list")
    @RequirePermission("user:read")
    public Result<List<User>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(userService.list(keyword));
    }

    /** POST /api/user/add  body: { username, phone, role } */
    @PostMapping("/add")
    @RequirePermission("user:create")
    public Result<User> add(@Valid @RequestBody UserRequest request) {
        return Result.ok("新增成功", userService.add(request));
    }

    /** POST /api/user/edit  body: { id, username, phone, role } */
    @PostMapping("/edit")
    @RequirePermission("user:update")
    public Result<User> edit(@Valid @RequestBody UserRequest request) {
        return Result.ok("编辑成功", userService.edit(request));
    }

    /** POST /api/user/delete  body: { id } */
    @PostMapping("/delete")
    @RequirePermission("user:delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object raw = body.get("id");
        if (raw == null) {
            throw new BizException(400, "缺少用户 id");
        }
        Long id;
        try {
            id = Long.valueOf(String.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new BizException(400, "用户 id 必须是数字");
        }
        userService.delete(id);
        return Result.ok("删除成功", null);
    }
}
