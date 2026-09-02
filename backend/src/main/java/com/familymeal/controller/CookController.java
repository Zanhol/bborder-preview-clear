package com.familymeal.controller;

import com.familymeal.service.CookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cook")
public class CookController {

    private final CookService cookService;

    public CookController(CookService cookService) {
        this.cookService = cookService;
    }

    /** 当前今日做饭人（无需登录，登录页需要展示） */
    @GetMapping("/current")
    public ResponseEntity<?> current() {
        return ResponseEntity.ok(Map.of("cookWho", cookService.getCookWho()));
    }

    /** 切换今日做饭人（需登录，登录后调用：登录即选做饭人） */
    @PostMapping("/switch")
    public ResponseEntity<?> switchCook(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        try {
            String who = body.get("who");
            Long userId = (Long) request.getAttribute("userId");
            return ResponseEntity.ok(cookService.switchCook(who, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
