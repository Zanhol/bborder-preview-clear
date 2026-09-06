/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("order_items")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishName;
    private String spiciness;
    private Integer quantity;
}
