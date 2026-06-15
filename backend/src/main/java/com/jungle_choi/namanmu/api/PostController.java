package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.api.dto.CategoryCountResponse;
import com.jungle_choi.namanmu.api.dto.PostPageResponse;
import com.jungle_choi.namanmu.api.dto.PostResponse;
import com.jungle_choi.namanmu.api.mapper.PostResponseMapper;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.post.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PostController {

    private final PostService postService;
    private final PostResponseMapper postResponseMapper;

    public PostController(
            PostService postService,
            PostResponseMapper postResponseMapper) {
        this.postService = postService;
        this.postResponseMapper = postResponseMapper;
    }

    @GetMapping
    public PostPageResponse listPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "All") String category,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PostService.PostPageResult postPage = postService.listPosts(
                keyword,
                category,
                tag,
                page,
                size);

        return new PostPageResponse(
                postPage.posts()
                        .stream()
                        .map(postResponseMapper::toPostResponse)
                        .toList(),
                postPage.page(),
                postPage.size(),
                postPage.totalElements(),
                postPage.totalPages(),
                postPage.categoryCounts()
                        .stream()
                        .map((categoryCount) -> new CategoryCountResponse(
                                categoryCount.category(),
                                categoryCount.count()))
                        .toList());
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId) {
        Post post = postService.getPublishedPost(user, postId);

        return postResponseMapper.toPostResponse(post);
    }

    @PostMapping
    public PostResponse createPost(
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreatePostRequest request) {
        Post post = postService.createPost(author, toCommand(request));

        return postResponseMapper.toPostResponse(post);
    }

    @PatchMapping("/{postId}")
    public PostResponse updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        Post post = postService.updatePost(user, postId, toCommand(request));

        return postResponseMapper.toPostResponse(post);
    }

    @DeleteMapping("/{postId}")
    public void deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId) {
        postService.deletePost(user, postId);
    }

    private static PostService.SavePostCommand toCommand(CreatePostRequest request) {
        return new PostService.SavePostCommand(
                request.category(),
                request.title(),
                request.content(),
                request.tags());
    }

    private static PostService.SavePostCommand toCommand(UpdatePostRequest request) {
        return new PostService.SavePostCommand(
                request.category(),
                request.title(),
                request.content(),
                request.tags());
    }

    public record CreatePostRequest(
            @NotBlank(message = "category is required.")
            @Size(max = 30, message = "category must be 30 characters or fewer.")
            String category,
            @NotBlank(message = "title is required.")
            @Size(max = 120, message = "title must be 120 characters or fewer.")
            String title,
            @NotBlank(message = "content is required.")
            @Size(max = 5000, message = "content must be 5000 characters or fewer.")
            String content,
            List<@Size(max = 30, message = "tag must be 30 characters or fewer.") String> tags) {
    }

    public record UpdatePostRequest(
            @NotBlank(message = "category is required.")
            @Size(max = 30, message = "category must be 30 characters or fewer.")
            String category,
            @NotBlank(message = "title is required.")
            @Size(max = 120, message = "title must be 120 characters or fewer.")
            String title,
            @NotBlank(message = "content is required.")
            @Size(max = 5000, message = "content must be 5000 characters or fewer.")
            String content,
            List<@Size(max = 30, message = "tag must be 30 characters or fewer.") String> tags) {
    }
}
