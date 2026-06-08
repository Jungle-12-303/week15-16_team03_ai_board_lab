package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PostController {

    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PostResponse> listPosts() {
        return postRepository.findAllByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)
                .stream()
                .map(PostController::toResponse)
                .toList();
    }

    @PostMapping
    public PostResponse createPost(@RequestBody CreatePostRequest request) {
        User author = findOrCreateLocalUser(request.author());
        Post post = Post.create(author, request.category(), request.title(), request.content());
        Post savedPost = postRepository.save(post);

        return toResponse(savedPost);
    }

    private User findOrCreateLocalUser(String authorName) {
        String localEmail = "local-" + Integer.toHexString(authorName.hashCode())
                + "@project-alpha.local";

        return userRepository.findByEmail(localEmail)
                .orElseGet(() -> userRepository.save(User.createLocalUser(authorName)));
    }

    private static PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getName(),
                post.getCategory(),
                formatDateTime(post.getCreatedAt()),
                post.getTitle(),
                post.getContent(),
                List.of(),
                List.of());
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        return dateTime.format(RESPONSE_DATE_FORMATTER);
    }

    public record PostResponse(
            Long id,
            String author,
            String category,
            String createdAt,
            String title,
            String content,
            List<String> tags,
            List<CommentResponse> comments) {
    }

    public record CommentResponse(Long id, String author, String content) {
    }

    public record CreatePostRequest(
            String author,
            String category,
            String title,
            String content,
            List<String> tags) {
    }
}
