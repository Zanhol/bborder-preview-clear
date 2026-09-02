package com.familymeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymeal.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
