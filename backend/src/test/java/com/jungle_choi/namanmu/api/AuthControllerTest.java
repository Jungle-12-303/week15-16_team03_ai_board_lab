package com.jungle_choi.namanmu.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSetsHttpOnlyCookieAndCookieRestoresCurrentUser() throws Exception {
        String username = "cookie-user";
        String password = "password123";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(username));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(
                        JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                        true))
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
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(
                        JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                        0));
    }
}
