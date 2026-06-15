package com.example.aiknowledgeboard.auth;

import com.example.aiknowledgeboard.user.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new EntityNotFoundException("로그인이 필요합니다.");
        }
        return user;
    }
}
