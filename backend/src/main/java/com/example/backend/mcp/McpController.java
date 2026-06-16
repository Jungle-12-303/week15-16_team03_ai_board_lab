package com.example.backend.mcp;

import com.example.backend.user.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpWeatherDraftService mcpWeatherDraftService;
    private final JwtTokenProvider jwtTokenProvider;

    public McpController(
        McpWeatherDraftService mcpWeatherDraftService,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.mcpWeatherDraftService = mcpWeatherDraftService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/weather-draft")
    public McpWeatherDraftResponse createWeatherDraft(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody McpWeatherDraftRequest request
    ) {
        String loginId = extractLoginId(authorizationHeader);
        return mcpWeatherDraftService.createWeatherDraft(
            request.getCity(),
            request.getForecastDays(),
            loginId
        );
    }

    private String extractLoginId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization 헤더 형식이 올바르지 않습니다.");
        }

        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        return jwtTokenProvider.getLoginId(token);
    }
}
