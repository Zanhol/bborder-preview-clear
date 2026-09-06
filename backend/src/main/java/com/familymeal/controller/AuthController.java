/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.dto.LoginRequest;
import com.familymeal.dto.LoginResponse;
import com.familymeal.entity.User;
import com.familymeal.mapper.UserMapper;
import com.familymeal.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @Value("${app.admin.reset-key:}")
    private String resetKey;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 当前登录用户的完整信息（含 DIY 背景） */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if ("admin".equals(request.getAttribute("role"))) {
            return ResponseEntity.ok(Map.of("id", 0L, "role", "admin", "nickname", "管理员"));
        }
        User u = userMapper.selectById(userId);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "role", u.getRole() == null ? "" : u.getRole(),
                "nickname", u.getNickname() == null ? "" : u.getNickname(),
                "avatarUrl", u.getAvatarUrl() == null ? "" : u.getAvatarUrl(),
                "backgroundUrl", u.getBackgroundUrl() == null ? "" : u.getBackgroundUrl()));
    }

    /** 更新当前用户的 DIY 背景 */
    @PutMapping("/me/background")
    public ResponseEntity<?> updateBackground(@RequestBody Map<String, String> body,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if ("admin".equals(request.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "管理员无背景设置"));
        }
        String bg = body.get("backgroundUrl");
        if (bg == null || bg.length() > 512) {
            return ResponseEntity.badRequest().body(Map.of("error", "背景参数无效"));
        }
        int n = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId).set(User::getBackgroundUrl, bg));
        if (n == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("success", true, "backgroundUrl", bg));
    }

    /**
     * 重置所有微信身份绑定（开发测试/上线前重新分配夫妻席位用）。
     * 需在请求头携带 X-Admin-Key 匹配 app.admin.reset-key；未配置密钥则禁用。
     */
    @PostMapping("/reset-bindings")
    public ResponseEntity<?> resetBindings(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (resetKey == null || resetKey.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "重置绑定功能未启用"));
        }
        if (adminKey == null || !resetKey.equals(adminKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "管理密钥无效"));
        }
        int count = authService.resetBindings();
        return ResponseEntity.ok(Map.of("success", true, "reset", count));
    }

    /** 管理后台登录：口令换取 admin 角色的 JWT */
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.adminLogin(request.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
