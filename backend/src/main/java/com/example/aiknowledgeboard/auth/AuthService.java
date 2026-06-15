package com.example.aiknowledgeboard.auth;

import com.example.aiknowledgeboard.config.JwtTokenProvider;
import com.example.aiknowledgeboard.user.UserEntity;
import com.example.aiknowledgeboard.user.UserRepository;
import org.apache.catalina.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

//    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//        this.jwtTokenProvider = jwtTokenProvider;
//    }
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if(userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("이미 가입된 이메일임");
        }
        UserEntity user = userRepository.save(new UserEntity(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname().trim()
        ));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(
                jwtTokenProvider.createToken(user.getId(), user.getEmail()),
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
