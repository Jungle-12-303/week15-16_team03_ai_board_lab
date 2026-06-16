package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.service.mcp.GitHubFactCheckService;
import com.jungle_choi.namanmu.service.mcp.McpFactCheckService;
import com.jungle_choi.namanmu.service.mcp.WeatherFactCheckService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
public class PostFactCheckController {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final McpFactCheckService mcpFactCheckService;
    private final WeatherFactCheckService weatherFactCheckService;
    private final GitHubFactCheckService gitHubFactCheckService;

    public PostFactCheckController(
            PostRepository postRepository,
            PostTagRepository postTagRepository,
            McpFactCheckService mcpFactCheckService,
            WeatherFactCheckService weatherFactCheckService,
            GitHubFactCheckService gitHubFactCheckService) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.mcpFactCheckService = mcpFactCheckService;
        this.weatherFactCheckService = weatherFactCheckService;
        this.gitHubFactCheckService = gitHubFactCheckService;
    }

    @PostMapping("/{postId}/fact-check")
    public McpFactCheckService.UnifiedFactCheckResult checkFact(
            @PathVariable Long postId) {
        Post post = findPublishedPost(postId);
        List<String> tags = tagsForPost(post);

        return mcpFactCheckService.check(
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                tags);
    }

    @PostMapping("/{postId}/fact-check/weather")
    public WeatherFactCheckService.WeatherFactCheckResult checkWeatherFact(
            @PathVariable Long postId) {
        Post post = findPublishedPost(postId);
        List<String> tags = tagsForPost(post);

        return weatherFactCheckService.check(
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                tags);
    }

    @PostMapping("/{postId}/fact-check/github")
    public GitHubFactCheckService.GitHubFactCheckResult checkGitHubFact(
            @PathVariable Long postId) {
        Post post = findPublishedPost(postId);
        List<String> tags = tagsForPost(post);

        return gitHubFactCheckService.check(
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                tags);
    }

    private Post findPublishedPost(Long postId) {
        return postRepository.findById(postId)
                .filter((foundPost) -> foundPost.getStatus() == PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private List<String> tagsForPost(Post post) {
        return postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();
    }
}
