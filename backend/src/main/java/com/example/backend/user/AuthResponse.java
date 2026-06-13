package com.example.backend.user;

import lombok.Getter;

@Getter
public class AuthResponse {

    private String token;
    private String nickname;
    private String loginId;

    public AuthResponse(String token, String nickname, String loginId) {
        this.token = token;
        this.nickname = nickname;
        this.loginId = loginId;
    }
}