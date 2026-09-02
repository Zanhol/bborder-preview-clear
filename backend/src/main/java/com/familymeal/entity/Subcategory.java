package com.familymeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("subcategories")
public class Subcategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String catKey;
    private String subKey;
    private String label;
    private Integer sort;
}
