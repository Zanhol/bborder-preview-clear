/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dishes")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String imagePath;
    private String spiciness;
    private String category;
    private String subcategory;
    /** 菜品归属：husband / wife / both（公用），决定出现在谁的菜单里 */
    private String owner;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
