package com.familymeal.controller;

import com.familymeal.dto.LoginRequest;
import com.familymeal.dto.LoginResponse;
import com.familymeal.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.admin.reset-key:}")
    private String resetKey;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
