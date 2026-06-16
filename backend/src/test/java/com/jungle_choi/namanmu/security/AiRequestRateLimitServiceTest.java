package com.jungle_choi.namanmu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jungle_choi.namanmu.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiRequestRateLimitServiceTest {

    @Test
    void blocksRequestsOverLimitForSameUserAndScope() {
        AiRequestRateLimitService service = new AiRequestRateLimitService(2, 60);
        User user = User.createLocalUser("ai-rate-limit-user");

        service.assertAllowed(user, "draft");
        service.assertAllowed(user, "draft");

        assertThatThrownBy(() -> service.assertAllowed(user, "draft"))
                .isInstanceOfSatisfying(ResponseStatusException.class, (exception) ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void countsDifferentScopesSeparately() {
        AiRequestRateLimitService service = new AiRequestRateLimitService(1, 60);
        User user = User.createLocalUser("ai-rate-limit-scopes");

        service.assertAllowed(user, "similar-posts");

        assertThatThrownBy(() -> service.assertAllowed(user, "similar-posts"))
                .isInstanceOf(ResponseStatusException.class);
        service.assertAllowed(user, "draft");
    }
}
