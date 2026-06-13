package com.example.backend.user;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@CrossOrigin(origins = "*")
@RestController
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/api/users/signup")
    public UserResponse signUp(@RequestBody UserSignUpRequest request) {
        return userService.signUp(request);
    }

    @PostMapping("/api/users/login")
    public AuthResponse login(@RequestBody UserLoginRequest request) {
        return userService.login(request);
    }
    
    @GetMapping("/api/users/me")
    public String getMyLoginId(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        return jwtTokenProvider.getLoginId(token);
    }
    
}