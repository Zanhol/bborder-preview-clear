/*
 * Copyright 2026 深圳市甜梦屋科技有限公司
 * Licensed under the Apache License, Version 2.0.
 */

package com.familymeal.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String code;
    private String role;
    private String password;
}
