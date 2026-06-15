package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.agent.AgentRecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AgentController {

    private final AgentRecommendationService agentRecommendationService;

    public AgentController(AgentRecommendationService agentRecommendationService) {
        this.agentRecommendationService = agentRecommendationService;
    }

    @PostMapping("/missed-posts")
    public AgentRecommendationService.AgentRecommendationResult recommendMissedPosts(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) MissedPostsRequest request) {
        int limit = request == null ? 5 : request.limit();

        return agentRecommendationService.recommendMissedPosts(user, limit);
    }

    public record MissedPostsRequest(int limit) {
    }
}
