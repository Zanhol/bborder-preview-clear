/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String role;
    private String nickname;
    private String avatarUrl;
    private String backgroundUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
