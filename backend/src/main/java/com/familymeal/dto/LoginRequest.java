package com.familymeal.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String code;
    private String role;
    private String password;
}
