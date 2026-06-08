package com.jungle_choi.namanmu.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationSeconds;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        return createSignedToken(
                Map.of("alg", "HS256", "typ", "JWT"),
                Map.of(
                        "sub", user.getEmail(),
                        "name", user.getName(),
                        "iat", issuedAt,
                        "exp", expiresAt));
    }

    public String readEmailFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return readEmail(authorizationHeader.substring("Bearer ".length()));
    }

    private String readEmail(String token) {
        try {
            String[] tokenParts = token.split("\\.");

            if (tokenParts.length != 3) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            String unsignedToken = tokenParts[0] + "." + tokenParts[1];
            String expectedSignature = sign(unsignedToken);

            if (!expectedSignature.equals(tokenParts[2])) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            JsonNode payload = objectMapper.readTree(decode(tokenParts[1]));
            long expiresAt = payload.get("exp").asLong();

            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            return payload.get("sub").asText();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private String createSignedToken(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;

            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT token could not be created.", exception);
        }
    }

    private String sign(String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM);
        mac.init(secretKey);

        return encode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    }

    private static String encode(byte[] source) {
        return BASE64_URL_ENCODER.encodeToString(source);
    }

    private static byte[] decode(String source) {
        return BASE64_URL_DECODER.decode(source);
    }
}
