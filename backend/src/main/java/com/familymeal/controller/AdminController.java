package com.familymeal.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.entity.User;
import com.familymeal.mapper.UserMapper;
import com.familymeal.service.WeChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台用户/身份管理接口（仅 role=admin 可访问）。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final UserMapper userMapper;
    private final WeChatService weChatService;

    public AdminController(UserMapper userMapper, WeChatService weChatService) {
        this.userMapper = userMapper;
        this.weChatService = weChatService;
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "admin".equals(request.getAttribute("role"));
    }

    /** 列出所有用户与绑定状态 */
    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }
        List<Map<String, Object>> items = userMapper.selectList(null).stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("role", u.getRole());
                    m.put("nickname", u.getNickname());
                    m.put("openidBound", u.getOpenid() != null && !u.getOpenid().isEmpty());
                    m.put("openid", u.getOpenid());
                    m.put("createdAt", u.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /** 分配/调整用户角色（husband / wife） */
    @PutMapping("/{id}/role")
    public ResponseEntity<?> assignRole(@PathVariable Long id,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }
        String role = body.get("role");
        if (role == null || (!"husband".equals(role) && !"wife".equals(role))) {
            return ResponseEntity.badRequest().body(Map.of("error", "角色必须为 husband 或 wife"));
        }
        int n = userMapper.update(null,
                new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getRole, role));
        if (n == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 解绑指定用户的微信 openid（单个席位重新开放） */
    @PostMapping("/{id}/unbind")
    public ResponseEntity<?> unbind(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }
        var user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        weChatService.clearUserOpenId(id);
        userMapper.update(null,
                new LambdaUpdateWrapper<User>().eq(User::getId, id).set(User::getOpenid, null));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
