package com.example.aiknowledgeboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                /*
                http.csrf(new Customizer <CsrfConfigurer<HttpSecurity>>
                { public void customizer(CsrfConfigurer<HttpSecurity> csrf)
                    { csrf.disable();
                    }
                 });
                */
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                /* UsernamePasswordAuthenticationFilter보다 앞에서 JwtAuthenticationFilter를 실행해라 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    //다른 주소에서 온 브라우저 요청을 허용할지 정하는 규칙
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        /*http://localhost:5173에서 오는 브라우저 요청 허용, http://localhost:3000에서 오는 브라우저 요청 허용*/

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        /* 허용할 HTTP 메서드를 정합니다.*/
        configuration.setAllowedHeaders(List.of("*"));
        /* 모든 요청 헤더를 허용합니다 */
        configuration.setAllowCredentials(true);
        /* 인증 정보를 포함한 요청을 허용한다는 뜻입니다. */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        /* URL 경로별로 CORS 설정을 등록할 수 있는 객체를 만듭니다. */
        source.registerCorsConfiguration("/**", configuration);
        /* 모든 경로에 위에서 만든 CORS 설정 */
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
