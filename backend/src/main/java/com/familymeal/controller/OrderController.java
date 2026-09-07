/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.controller;

import com.familymeal.dto.OrderRequest;
import com.familymeal.dto.OrderResponse;
import com.familymeal.service.CookService;
import com.familymeal.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CookService cookService;

    public OrderController(OrderService orderService, CookService cookService) {
        this.orderService = orderService;
        this.cookService = cookService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderRequest request,
                                     HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("role");
        // 下单端 = 今日非做饭人（点餐人）
        if (cookService.isCook(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "今天是您做饭，让对方点菜下单"));
        }
        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponse response = orderService.create(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ResponseEntity.ok(orderService.list(userId, role));
    }

    @PutMapping("/{id}/received")
    public ResponseEntity<?> markReceived(@PathVariable Long id,
                                           HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!cookService.isCook(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "仅今日做饭人可操作"));
        }
        orderService.markReceived(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        // 任意状态可删：下单本人或今日做饭人
        if (!orderService.isOwner(id, userId) && !cookService.isCook(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "仅下单人或今日做饭人可删除"));
        }
        orderService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
