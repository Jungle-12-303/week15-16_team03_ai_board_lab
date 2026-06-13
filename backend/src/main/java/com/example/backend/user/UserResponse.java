package com.example.backend.user;

import lombok.Getter;

@Getter
public class UserResponse {

    private Long id;
    private String loginId;
    private String nickname;

    public UserResponse(Long id, String loginId, String nickname) {
        this.id = id;
        this.loginId = loginId;
        this.nickname = nickname;
    }
}