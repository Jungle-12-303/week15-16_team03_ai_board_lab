package com.example.backend.rag;

import com.example.backend.user.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final PostEmbeddingService postEmbeddingService;
    private final JwtTokenProvider jwtTokenProvider;

    public RagController(
        PostEmbeddingService postEmbeddingService,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.postEmbeddingService = postEmbeddingService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/status")
    public RagStatusResponse getStatus(
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        extractLoginId(authorizationHeader);
        return postEmbeddingService.getRagStatus();
    }

    @PostMapping("/reindex/posts")
    public RagReindexResponse reindexPosts(
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        String loginId = extractLoginId(authorizationHeader);
        return postEmbeddingService.reindexAllPosts(loginId);
    }

    @PostMapping("/ask")
    public RagAskResponse askQuestion(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody RagAskRequest request
    ) {
        String loginId = extractLoginId(authorizationHeader);
        return postEmbeddingService.askQuestion(request.getQuestion(), loginId);
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
