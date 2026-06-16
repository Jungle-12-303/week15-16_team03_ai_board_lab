package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.config.AppSecurityProperties;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.security.JwtAuthenticationFilter;
import com.jungle_choi.namanmu.security.JwtTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AppSecurityProperties appSecurityProperties;
    private final long expirationSeconds;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AppSecurityProperties appSecurityProperties,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
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

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAccessTokenCookie(user).toString())
                .body(toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearAccessTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return toResponse(user);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getName());
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
