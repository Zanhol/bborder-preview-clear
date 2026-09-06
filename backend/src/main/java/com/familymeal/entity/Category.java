/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("categories")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String catKey;
    private String label;
    private Integer sort;
}
