package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.service.WeatherFactCheckService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PostFactCheckController {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final WeatherFactCheckService weatherFactCheckService;

    public PostFactCheckController(
            PostRepository postRepository,
            PostTagRepository postTagRepository,
            WeatherFactCheckService weatherFactCheckService) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.weatherFactCheckService = weatherFactCheckService;
    }

    @PostMapping("/{postId}/fact-check/weather")
    public WeatherFactCheckService.WeatherFactCheckResult checkWeatherFact(
            @PathVariable Long postId) {
        Post post = postRepository.findById(postId)
                .filter((foundPost) -> foundPost.getStatus() == PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<String> tags = postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();

        return weatherFactCheckService.check(
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                tags);
    }
}
