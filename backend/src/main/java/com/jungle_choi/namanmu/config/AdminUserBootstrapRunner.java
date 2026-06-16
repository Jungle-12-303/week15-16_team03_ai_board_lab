package com.jungle_choi.namanmu.config;

import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrapRunner.class);

    private final UserRepository userRepository;
    private final String adminUsernames;

    public AdminUserBootstrapRunner(
            UserRepository userRepository,
            @Value("${app.admin.usernames:}") String adminUsernames) {
        this.userRepository = userRepository;
        this.adminUsernames = adminUsernames;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUsernames == null || adminUsernames.isBlank()) {
            return;
        }

        Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .filter((username) -> !username.isBlank())
                .distinct()
                .forEach(this::promoteIfUserExists);
    }

    private void promoteIfUserExists(String username) {
        userRepository.findByEmail(User.accountEmail(username))
                .ifPresentOrElse(
                        (user) -> {
                            user.promoteToAdmin();
                            log.info("Promoted configured admin user. username={}", username);
                        },
                        () -> log.warn(
                                "Configured admin user does not exist yet. username={}",
                                username));
    }
}
