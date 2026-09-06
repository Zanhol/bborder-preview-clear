package com.familymeal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymeal.dto.OrderRequest;
import com.familymeal.dto.OrderResponse;
import com.familymeal.entity.Dish;
import com.familymeal.entity.Order;
import com.familymeal.entity.OrderItem;
import com.familymeal.mapper.DishMapper;
import com.familymeal.mapper.OrderItemMapper;
import com.familymeal.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final WeChatService weChatService;
    private final CookService cookService;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                        DishMapper dishMapper, WeChatService weChatService, CookService cookService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.weChatService = weChatService;
        this.cookService = cookService;
    }

    @Transactional
    public OrderResponse create(Long userId, OrderRequest request) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("pending");
        // 连续编号：当前最大编号+1，无则从1开始
        Integer maxNum = orderMapper.selectList(null).stream()
                .map(Order::getOrderNumber).filter(n -> n != null)
                .max(Integer::compareTo).orElse(0);
        order.setOrderNumber(maxNum + 1);
        orderMapper.insert(order);

        List<OrderResponse.OrderItemInfo> items = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        int maxSpice = 0;
        for (OrderRequest.DishItem di : request.getDishes()) {
            Dish dish = dishMapper.selectById(di.getDishId());
            if (dish == null || "deleted".equals(dish.getStatus())) continue;
            String spice = di.getSpiciness() != null ? di.getSpiciness() : (dish.getSpiciness() != null ? dish.getSpiciness() : "none");

            if (names.length() > 0) names.append("、");
            names.append(dish.getName());
            if ("female_baby".equals(spice)) maxSpice = Math.max(maxSpice, 2);
            else if ("male_baby".equals(spice)) maxSpice = Math.max(maxSpice, 1);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(di.getDishId());
            item.setDishName(dish.getName());
            item.setSpiciness(spice);
            item.setQuantity(1);
            orderItemMapper.insert(item);

            items.add(new OrderResponse.OrderItemInfo(di.getDishId(), dish.getName(), spice, 1));
        }

        // 微信通知今日做饭人（P2-1：下单通知路由给当天做饭的人）
        String spiceText = maxSpice == 2 ? "女宝辣" : maxSpice == 1 ? "男宝辣" : "不辣";
        String timeText = order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MM月dd日 HH:mm"));
        weChatService.sendOrderNotify(cookService.getCookUserId(), String.valueOf(order.getId()), names.toString(), spiceText, timeText);

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setItems(items);
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    public List<OrderResponse> list(Long userId, String role) {
        List<Order> orders;
        if (cookService.isCook(role)) {
            // 做饭人：看全部订单（接单端）
            orders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt));
        } else {
            // 点餐人：只看自己下的单
            orders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getUserId, userId)
                            .orderByDesc(Order::getCreatedAt));
        }

        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));

            OrderResponse resp = new OrderResponse();
            resp.setId(order.getId());
            resp.setOrderNumber(order.getOrderNumber());
            resp.setStatus(order.getStatus());
            resp.setCreatedAt(order.getCreatedAt());
            resp.setItems(items.stream()
                    .map(i -> new OrderResponse.OrderItemInfo(i.getDishId(), i.getDishName(), i.getSpiciness(), i.getQuantity()))
                    .collect(Collectors.toList()));
            return resp;
        }).collect(Collectors.toList());
    }

    public void markReceived(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        order.setStatus("received");
        orderMapper.updateById(order);

        // 微信通知下单人（点餐端）：老婆已收到订单
        java.util.List<OrderItem> items = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId));
        StringBuilder names = new StringBuilder();
        for (OrderItem it : items) {
            if (names.length() > 0) names.append("、");
            names.append(it.getDishName());
        }
        String timeText = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MM月dd日 HH:mm"));
        weChatService.sendReceivedNotify(order.getUserId(),
                String.valueOf(order.getOrderNumber()), names.toString(), timeText);
    }

    @Transactional
    public void delete(Long orderId) {
        orderItemMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        orderMapper.deleteById(orderId);
    }
}
