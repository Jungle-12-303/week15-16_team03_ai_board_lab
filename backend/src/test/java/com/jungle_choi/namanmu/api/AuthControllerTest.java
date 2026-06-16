package com.jungle_choi.namanmu.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jungle_choi.namanmu.security.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "project_alpha_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSetsHttpOnlyCookieAndCookieRestoresCurrentUser() throws Exception {
        String username = "cookie-user";
        String password = "password123";

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(username));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(
                        JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                        true))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(jsonPath("$.name").value(username))
                .andReturn();

        Cookie accessTokenCookie = loginResult.getResponse()
                .getCookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME);

        assertThat(accessTokenCookie).isNotNull();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(username));
    }

    @Test
    void logoutClearsAccessTokenCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(
                        JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                        0))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));
    }

    @Test
    void refreshRotatesRefreshTokenAndIssuesNewAccessCookie() throws Exception {
        String username = "refresh-user";
        String password = "password123";

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie firstRefreshCookie = loginResult.getResponse()
                .getCookie(REFRESH_TOKEN_COOKIE_NAME);

        assertThat(firstRefreshCookie).isNotNull();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME))
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(jsonPath("$.name").value(username))
                .andReturn();
        Cookie secondRefreshCookie = refreshResult.getResponse()
                .getCookie(REFRESH_TOKEN_COOKIE_NAME);

        assertThat(secondRefreshCookie).isNotNull();
        assertThat(secondRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(firstRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointReturnsTokenForBrowserRequests() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void mutatingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }
}
