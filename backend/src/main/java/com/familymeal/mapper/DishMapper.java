package com.familymeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymeal.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
