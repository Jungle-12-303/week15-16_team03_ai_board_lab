package com.jungle_choi.namanmu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class JwtTokenServiceTest {

    private static final String SECRET = "project-alpha-test-secret-at-least-32-bytes";
    private static final Base64.Encoder BASE64_URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenService jwtTokenService =
            new JwtTokenService(objectMapper, SECRET, 3600, false);

    @Test
    void createTokenCanBeReadFromRawToken() {
        User user = User.createRegisteredUser("cedis", "hashed-password");

        String token = jwtTokenService.createToken(user);

        assertThat(jwtTokenService.readEmailFromToken(token))
                .isEqualTo(user.getEmail());
    }

    @Test
    void readEmailRejectsTokenWithUnexpectedAlgorithmHeader() {
        String token = createSignedToken(
                Map.of("alg", "none", "typ", "JWT"),
                Map.of(
                        "sub", "account-test@project-alpha.local",
                        "iat", Instant.now().getEpochSecond(),
                        "exp", Instant.now().plusSeconds(3600).getEpochSecond()));

        assertThatThrownBy(() -> jwtTokenService.readEmailFromToken(token))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void readEmailRejectsTokenWithTamperedSignature() {
        User user = User.createRegisteredUser("cedis", "hashed-password");
        String token = jwtTokenService.createToken(user);
        String[] tokenParts = token.split("\\.");
        String tamperedSignature = (tokenParts[2].startsWith("A") ? "B" : "A")
                + tokenParts[2].substring(1);
        String tamperedToken = tokenParts[0] + "." + tokenParts[1] + "." + tamperedSignature;

        assertThatThrownBy(() -> jwtTokenService.readEmailFromToken(tamperedToken))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void constructorRejectsShortHs256Secret() {
        assertThatThrownBy(() -> new JwtTokenService(objectMapper, "short-secret", 3600, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void constructorRejectsLocalDevelopmentSecretInProductionMode() {
        assertThatThrownBy(() -> new JwtTokenService(
                        objectMapper,
                        "project-alpha-local-development-secret-change-me",
                        3600,
                        true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be changed in production mode");
    }

    private String createSignedToken(
            Map<String, Object> header,
            Map<String, Object> payload) {
        try {
            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;

            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String sign(String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));

        return encode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    }

    private static String encode(byte[] source) {
        return BASE64_URL_ENCODER.encodeToString(source);
    }
}
