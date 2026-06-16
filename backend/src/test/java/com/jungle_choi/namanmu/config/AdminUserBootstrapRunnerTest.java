package com.jungle_choi.namanmu.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.domain.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

class AdminUserBootstrapRunnerTest {

    @Test
    void promotesExistingConfiguredUsersToAdmin() throws Exception {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User user = User.createRegisteredUser("admin-user", "hashed-password");
        AdminUserBootstrapRunner runner =
                new AdminUserBootstrapRunner(userRepository, "admin-user, admin-user");

        when(userRepository.findByEmail(User.accountEmail("admin-user")))
                .thenReturn(Optional.of(user));

        runner.run(new DefaultApplicationArguments());

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository, times(1)).findByEmail(User.accountEmail("admin-user"));
    }
}
