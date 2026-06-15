package com.example.aiknowledgeboard.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private final String secret;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            ObjectMapper objectMapper
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.objectMapper = objectMapper;
    }

    public String createToken(Long userId, String email) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", userId.toString());
            payload.put("email", email);
            payload.put("exp", Instant.now().plusMillis(expirationMs).getEpochSecond());

            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + sign(signingInput);
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 토큰 생성에 실패했습니다.", ex);
        }
    }

    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT 형식이 올바르지 않습니다.");
            }
            String signingInput = parts[0] + "." + parts[1];
            if (!constantTimeEquals(parts[2], sign(signingInput))) {
                throw new IllegalArgumentException("JWT 서명이 올바르지 않습니다.");
            }
            Map<String, Object> payload = objectMapper.readValue(
                    base64UrlDecoder().decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("JWT 토큰이 만료되었습니다.");
            }
            return Long.valueOf(payload.get("sub").toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException("JWT 토큰을 검증할 수 없습니다.", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return base64UrlEncoder().encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64UrlEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private Base64.Encoder base64UrlEncoder() {
        return Base64.getUrlEncoder().withoutPadding();
    }

    private Base64.Decoder base64UrlDecoder() {
        return Base64.getUrlDecoder();
    }

    // 와 지린다 모든 바이트 다 비교해보고 맞으면 true 반환, 비교시간 보고 길이 추측해서 해킹하는 애들때문에 비교시간 일정하게 하려고
    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
