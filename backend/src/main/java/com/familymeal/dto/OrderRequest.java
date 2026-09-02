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
