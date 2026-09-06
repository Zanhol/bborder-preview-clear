/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<DishItem> dishes;

    @Data
    public static class DishItem {
        private Long dishId;
        private String spiciness;
    }
}
