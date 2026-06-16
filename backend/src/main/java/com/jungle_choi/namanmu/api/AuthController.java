package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.config.AppSecurityProperties;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.security.JwtAuthenticationFilter;
import com.jungle_choi.namanmu.security.JwtTokenService;
import com.jungle_choi.namanmu.security.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "project_alpha_refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final AppSecurityProperties appSecurityProperties;
    private final long expirationSeconds;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            CookieCsrfTokenRepository csrfTokenRepository,
            AppSecurityProperties appSecurityProperties,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.appSecurityProperties = appSecurityProperties;
        this.expirationSeconds = expirationSeconds;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@Valid @RequestBody AuthRequest request) {
        String username = request.username().trim();
        String password = request.password();
        String accountEmail = User.accountEmail(username);

        if (userRepository.existsByEmail(accountEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        User user = User.createRegisteredUser(
                username,
                passwordEncoder.encode(password));
        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        String username = request.username().trim();
        String password = request.password();
        String accountEmail = User.accountEmail(username);
        User user = userRepository.findByEmail(accountEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);

        return ResponseEntity.ok()
                .headers((headers) -> addAuthCookies(headers, user, refreshToken.value()))
                .body(toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        readCookie(request, REFRESH_TOKEN_COOKIE_NAME)
                .ifPresent(refreshTokenService::revoke);

        return ResponseEntity.noContent()
                .headers((headers) -> {
                    headers.add(HttpHeaders.SET_COOKIE, clearAccessTokenCookie().toString());
                    headers.add(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
                })
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = readCookie(request, REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.rotate(rawRefreshToken);

        return ResponseEntity.ok()
                .headers((headers) ->
                        addAuthCookies(headers, refreshToken.user(), refreshToken.value()))
                .body(toResponse(refreshToken.user()));
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return toResponse(user);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.loadToken(request);

        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
        }

        csrfTokenRepository.saveToken(csrfToken, request, response);

        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getName());
    }

    private void addAuthCookies(HttpHeaders headers, User user, String refreshToken) {
        headers.add(HttpHeaders.SET_COOKIE, createAccessTokenCookie(user).toString());
        headers.add(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refreshToken).toString());
    }

    private ResponseCookie createAccessTokenCookie(User user) {
        return ResponseCookie.from(
                        JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                        jwtTokenService.createToken(user))
                .httpOnly(true)
                .secure(appSecurityProperties.cookieSecure())
                .sameSite(appSecurityProperties.normalizedCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(expirationSeconds))
                .build();
    }

    private ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(appSecurityProperties.cookieSecure())
                .sameSite(appSecurityProperties.normalizedCookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(appSecurityProperties.cookieSecure())
                .sameSite(appSecurityProperties.normalizedCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTokenService.expirationSeconds()))
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(appSecurityProperties.cookieSecure())
                .sameSite(appSecurityProperties.normalizedCookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private static Optional<String> readCookie(
            HttpServletRequest request,
            String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }

        return Optional.empty();
    }

    public record AuthRequest(
            @NotBlank(message = "username is required.")
            String username,
            @NotBlank(message = "password is required.")
            @Size(min = 6, message = "password must be at least 6 characters.")
            String password) {
    }

    public record AuthResponse(String name) {
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
