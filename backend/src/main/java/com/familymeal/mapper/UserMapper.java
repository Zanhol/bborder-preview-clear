/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymeal.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
