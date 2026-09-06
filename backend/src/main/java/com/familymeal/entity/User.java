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
