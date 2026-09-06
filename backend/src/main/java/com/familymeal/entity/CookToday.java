/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cook_today")
public class CookToday {
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 今日做饭人：husband / wife */
    private String cookWho;
    /** 做饭状态：cooking / done */
    private String cookStatus;
    private LocalDateTime switchedAt;
    /** 切换者用户ID */
    private Long switchedBy;
}
