package com.jungle_choi.namanmu.security;

import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "project_alpha_access_token";

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(token);
        } catch (ResponseStatusException exception) {
            writeUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String email = jwtTokenService.readEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private static void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":401,"code":"UNAUTHORIZED","message":"Unauthorized","errors":[]}
                """);
    }
}
