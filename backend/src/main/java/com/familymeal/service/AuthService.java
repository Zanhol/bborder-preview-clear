package com.familymeal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.dto.LoginRequest;
import com.familymeal.dto.LoginResponse;
import com.familymeal.entity.User;
import com.familymeal.mapper.UserMapper;
import com.familymeal.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WeChatService weChatService;
    private final String adminPassword;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, WeChatService weChatService,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.weChatService = weChatService;
        this.adminPassword = adminPassword;
    }

    public LoginResponse login(LoginRequest request) {
        User user;

        if (request.getCode() != null && !request.getCode().isEmpty()) {
            // 小程序微信登录
            String openid = weChatService.codeToOpenId(request.getCode());
            if (openid == null) throw new RuntimeException("获取openid失败");

            // 1. 按 openid 找已有绑定
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));

            // 2. 首次使用：按「老公 → 老婆」顺序占用第一个空席位
            if (user == null) {
                user = firstOpenSeatFor("husband");
                if (user == null) {
                    user = firstOpenSeatFor("wife");
                }
                if (user != null) {
                    user.setOpenid(openid);
                    userMapper.updateById(user);
                }
            }
            if (user == null) {
                throw new RuntimeException("夫妻名额已满，请先用H5端登录一次");
            }
            weChatService.saveUserOpenId(user.getId(), openid);
        } else if (request.getRole() != null) {
            // H5 模拟登录
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getRole, request.getRole()));
            if (user == null) {
                throw new RuntimeException("角色不存在: " + request.getRole());
            }
        } else {
            throw new RuntimeException("请提供 code 或 role");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new LoginResponse(token,
                new LoginResponse.UserInfo(user.getId(), user.getRole(), user.getNickname()));
    }

    /** 查找指定角色中尚未绑定微信 openid 的用户（空席位） */
    private User firstOpenSeatFor(String role) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, role)
                        .isNull(User::getOpenid)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    /**
     * 清空所有用户的微信 openid 绑定，使席位重新开放（开发测试/上线前重置用）。
     * 同时清理内存中的 openid 缓存。
     * @return 被重置的用户数
     */
    public int resetBindings() {
        List<User> bound = userMapper.selectList(new LambdaQueryWrapper<User>().isNotNull(User::getOpenid));
        for (User u : bound) {
            weChatService.clearUserOpenId(u.getId());
        }
        return userMapper.update(null,
                new LambdaUpdateWrapper<User>().set(User::getOpenid, null));
    }

    /**
     * 管理后台登录：口令换取 JWT（角色 admin，不占用夫妻席位）。
     * admin 口令未配置或口令不匹配时抛异常。
     */
    public LoginResponse adminLogin(String password) {
        if (adminPassword == null || adminPassword.isEmpty()) {
            throw new RuntimeException("管理后台登录未启用");
        }
        if (password == null || !adminPassword.equals(password)) {
            throw new RuntimeException("管理员口令无效");
        }
        String token = jwtUtil.generateToken(0L, "admin");
        return new LoginResponse(token,
                new LoginResponse.UserInfo(0L, "admin", "管理员"));
    }
}
