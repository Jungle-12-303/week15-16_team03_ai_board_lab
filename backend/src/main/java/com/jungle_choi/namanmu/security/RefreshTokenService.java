package com.jungle_choi.namanmu.security;

import com.jungle_choi.namanmu.domain.auth.RefreshToken;
import com.jungle_choi.namanmu.domain.auth.RefreshTokenRepository;
import com.jungle_choi.namanmu.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final Base64.Encoder BASE64_URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.refresh-token.expiration-seconds}") long expirationSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = createRawToken();
        RefreshToken refreshToken = RefreshToken.create(
                user,
                hash(rawToken),
                LocalDateTime.now().plusSeconds(expirationSeconds));

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(rawToken, user);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!refreshToken.isActive(now)) {
            refreshToken.revoke(now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        User user = refreshToken.getUser();
        refreshToken.revoke(now);

        return issue(user);
    }

    @Transactional
    public void revoke(String rawToken) {
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent((refreshToken) -> refreshToken.revoke(now));
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private String createRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh token could not be hashed.", exception);
        }
    }

    public record IssuedRefreshToken(String value, User user) {
    }
}
