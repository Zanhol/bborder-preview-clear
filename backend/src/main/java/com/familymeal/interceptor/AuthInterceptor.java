package com.familymeal.interceptor;

import com.familymeal.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // 登录接口放行
        if (uri.equals("/api/auth/login")) {
            return true;
        }

        // 管理后台登录放行（由管理员口令保护）
        if (uri.equals("/api/auth/admin/login")) {
            return true;
        }

        // 身份绑定重置接口放行（由管理密钥 X-Admin-Key 保护的端点）
        if (uri.equals("/api/auth/reset-bindings")) {
            return true;
        }

        // 今日做饭人查询放行（登录页未登录时也需要展示）
        if (uri.equals("/api/cook/current")) {
            return true;
        }

        // 分类/子分类读取放行（小程序菜单与后台需未登录可读）
        if (uri.equals("/api/categories")) {
            return true;
        }

        // 全局 DIY 背景图公开读取放行
        if (uri.equals("/api/background")) {
            return true;
        }

        // 演示模式登录/seed/reset 放行（由 X-Demo-Key 保护）
        if (uri.equals("/demo/login") || uri.equals("/demo/seed") || uri.equals("/demo/reset")) {
            return true;
        }

        // 图片访问放行（点餐端查看菜单图片无需登录）
        if (uri.matches("/api/dishes/\\d+/(image|thumb)")) {
            return true;
        }

        // 上传文件的静态资源放行
        if (uri.startsWith("/uploads/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未登录\"}");
            return false;
        }

        try {
            String token = authHeader.substring(7);
            request.setAttribute("userId", jwtUtil.getUserId(token));
            request.setAttribute("role", jwtUtil.getRole(token));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Token无效\"}");
            return false;
        }
    }
}
