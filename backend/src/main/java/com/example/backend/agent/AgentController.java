package com.example.backend.agent;

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
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final JwtTokenProvider jwtTokenProvider;

    public AgentController(AgentService agentService, JwtTokenProvider jwtTokenProvider) {
        this.agentService = agentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/draft")
    public AgentDraftResponse createDraft(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody AgentDraftRequest request
    ) {
        String loginId = extractLoginId(authorizationHeader);
        return agentService.createDraft(request.getUserRequest(), loginId);
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
