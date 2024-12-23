package com.hmzadev.interactivechatbot.dao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private String refreshToken;

    public AuthenticationResponse(String token, String refreshToken) { // Add public access modifier
        this.token = token;
        this.refreshToken = refreshToken;
    }
}
