/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Integer orderNumber;
    private String status;
    private List<OrderItemInfo> items;
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    public static class OrderItemInfo {
        private Long dishId;
        private String dishName;
        private String spiciness;
        private Integer quantity;
    }
}
