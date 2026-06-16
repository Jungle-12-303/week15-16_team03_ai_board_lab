package com.jungle_choi.namanmu.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    private static final String JWT_ALGORITHM = "HS256";
    private static final String JWT_TYPE = "JWT";
    private static final int MINIMUM_HS256_SECRET_BYTES = 32;

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
        validateConfiguration();
    }

    public String createToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        return createSignedToken(
                Map.of("alg", JWT_ALGORITHM, "typ", JWT_TYPE),
                Map.of(
                        "sub", user.getEmail(),
                        "name", user.getName(),
                        "iat", issuedAt,
                        "exp", expiresAt));
    }

    public String readEmailFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return readEmail(token);
    }

    private String readEmail(String token) {
        try {
            String[] tokenParts = token.split("\\.", -1);

            if (tokenParts.length != 3) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            JsonNode header = objectMapper.readTree(decode(tokenParts[0]));
            validateHeader(header);

            String unsignedToken = tokenParts[0] + "." + tokenParts[1];
            String expectedSignature = sign(unsignedToken);

            if (!MessageDigest.isEqual(decode(expectedSignature), decode(tokenParts[2]))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            JsonNode payload = objectMapper.readTree(decode(tokenParts[1]));
            JsonNode subject = payload.get("sub");
            JsonNode expiration = payload.get("exp");

            if (subject == null || !subject.isTextual() || expiration == null || !expiration.canConvertToLong()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            if (Instant.now().getEpochSecond() >= expiration.asLong()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            return subject.asText();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateHeader(JsonNode header) {
        if (header == null
                || !JWT_ALGORITHM.equals(header.path("alg").asText())
                || !JWT_TYPE.equals(header.path("typ").asText())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateConfiguration() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_HS256_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256.");
        }

        if (expirationSeconds <= 0) {
            throw new IllegalStateException("app.jwt.expiration-seconds must be positive.");
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
