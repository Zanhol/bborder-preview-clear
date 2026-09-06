/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymeal.entity.Dish;
import com.familymeal.entity.Order;
import com.familymeal.entity.OrderItem;
import com.familymeal.entity.User;
import com.familymeal.mapper.DishMapper;
import com.familymeal.mapper.OrderItemMapper;
import com.familymeal.mapper.OrderMapper;
import com.familymeal.mapper.UserMapper;
import com.familymeal.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 纯净演示版：一键填充演示数据 / 一键清空。仅 DEMO_ENABLED=true 时启用。
 * H5 场景下无需微信凭据即可体验（demo 登录回落 H5 模拟登录）。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final boolean demoEnabled;

    public DemoController(DishMapper dishMapper, OrderMapper orderMapper,
                          OrderItemMapper orderItemMapper, UserMapper userMapper,
                          JwtUtil jwtUtil, @Value("${app.demo.enabled:false}") boolean demoEnabled) {
        this.dishMapper = dishMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.demoEnabled = demoEnabled;
    }

    private boolean disabled() {
        return !demoEnabled;
    }

    /** 演示模式一键填充：20 道演示菜（带归属）+ 1 个示例订单 */
    @PostMapping("/seed")
    public ResponseEntity<?> seed(@RequestHeader(value = "X-Demo-Key", required = false) String demoKey) {
        if (disabled() || !"demo".equals(demoKey)) return ResponseEntity.status(403).body(Map.of("error", "演示模式未启用或密钥错误"));
        // 幂等：先清空再填充，可反复玩
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>());
        orderMapper.delete(new LambdaQueryWrapper<Order>());
        dishMapper.delete(new LambdaQueryWrapper<Dish>());

        String[][] demoDishes = {
            // 老婆的拿手菜
            {"番茄鸡蛋面", "清淡暖胃，感冒来一碗", "none", "cooking", "staple", "wife"},
            {"可乐鸡翅", "小朋友口味，甜而不腻", "none", "cooking", "meat", "wife"},
            {"红烧排骨", "老公的拿手菜，软烂入味", "none", "cooking", "meat", "wife"},
            {"清炒时蔬", "当季蔬菜，清爽解腻", "none", "cooking", "vegetable", "wife"},
            {"香辣小龙虾", "老婆最爱，越辣越香", "female_baby", "cooking", "seafood", "wife"},
            {"玉米排骨汤", "炖了 1 小时，养生暖汤", "none", "cooking", "soup", "wife"},
            {"妈妈的蒸蛋", "入口即化，宝宝也爱吃", "none", "cooking", "breakfast", "wife"},
            {"韭菜炒蛋", "快手菜 5 分钟搞定", "none", "cooking", "vegetable", "wife"},
            // 老公的拿手菜
            {"黑椒牛排", "七分熟，肉汁满满", "male_baby", "cooking", "meat", "husband"},
            {"麻婆豆腐", "下饭神器，辣得过瘾", "male_baby", "cooking", "meat", "husband"},
            {"水煮鱼", "麻椒香味扑鼻", "female_baby", "cooking", "seafood", "husband"},
            {"卤肉饭", "台式风味，肥而不腻", "none", "cooking", "staple", "husband"},
            {"蒜蓉粉丝虾", "宴客硬菜", "none", "cooking", "seafood", "husband"},
            {"酸辣土豆丝", "家常下饭", "male_baby", "cooking", "vegetable", "husband"},
            {"皮蛋瘦肉粥", "暖胃早餐", "none", "cooking", "breakfast", "husband"},
            // 公用菜单
            {"白灼菜心", "清爽解腻，配菜必备", "none", "cooking", "vegetable", "both"},
            {"小米粥", "养胃好消化", "none", "cooking", "breakfast", "both"},
            {"水果沙拉", "饭后清口", "none", "cooking", "snack", "both"},
            // 懒人外卖
            {"披萨套餐", "周末偷懒神器", "none", "delivery", "", "husband"},
            {"炸鸡桶", "深夜馋嘴", "none", "delivery", "", "wife"},
        };
        for (String[] d : demoDishes) {
            Dish dish = new Dish();
            dish.setName(d[0]); dish.setDescription(d[1]); dish.setSpiciness(d[2]);
            dish.setCategory(d[3]); dish.setSubcategory(d[4]); dish.setOwner(d[5]);
            dish.setStatus("active");
            dishMapper.insert(dish);
        }

        // 一条示例订单：由 wife（点餐人端演示）下单，若做饭人是 husband 则 wife 下单合理
        Order order = new Order();
        order.setUserId(2L); order.setStatus("received"); order.setOrderNumber(1);
        order.setCreatedAt(LocalDateTime.now().minusHours(3));
        orderMapper.insert(order);
        Dish d = dishMapper.selectList(new LambdaQueryWrapper<Dish>().last("LIMIT 1")).get(0);
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId()); item.setDishId(d.getId());
        item.setDishName(d.getName()); item.setSpiciness(d.getSpiciness()); item.setQuantity(1);
        orderItemMapper.insert(item);

        return ResponseEntity.ok(Map.of("success", true,
                "dishes", demoDishes.length, "order", order.getId()));
    }

    /** 演示模式一键清空重置（不含 users/cook_today，保留身份） */
    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestHeader(value = "X-Demo-Key", required = false) String demoKey) {
        if (disabled() || !"demo".equals(demoKey)) return ResponseEntity.status(403).body(Map.of("error", "演示模式未启用或密钥错误"));
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>());
        orderMapper.delete(new LambdaQueryWrapper<Order>());
        dishMapper.delete(new LambdaQueryWrapper<Dish>());
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 演示模式：H5 免密登录指定角色（husband/wife），无需微信凭据 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   @RequestHeader(value = "X-Demo-Key", required = false) String demoKey) {
        if (disabled() || !"demo".equals(demoKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "演示模式未启用或密钥错误"));
        }
        String role = body.get("role");
        if (!"husband".equals(role) && !"wife".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("error", "角色须为 husband/wife"));
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getRole, role));
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "角色不存在"));
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "user",
                Map.of("id", user.getId(), "role", user.getRole(), "nickname", user.getNickname())));
    }
}
