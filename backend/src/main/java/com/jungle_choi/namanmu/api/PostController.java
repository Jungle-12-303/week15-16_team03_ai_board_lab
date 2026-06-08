package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.comment.Comment;
import com.jungle_choi.namanmu.domain.comment.CommentRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PostController {

    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public PostController(
            PostRepository postRepository,
            UserRepository userRepository,
            CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    @GetMapping
    public List<PostResponse> listPosts() {
        return postRepository.findAllByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public PostResponse createPost(@RequestBody CreatePostRequest request) {
        User author = findOrCreateLocalUser(request.author());
        Post post = Post.create(author, request.category(), request.title(), request.content());
        Post savedPost = postRepository.save(post);

        return toResponse(savedPost);
    }

    @PatchMapping("/{postId}")
    @Transactional
    public PostResponse updatePost(
            @PathVariable Long postId,
            @RequestBody UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow();

        post.update(request.category(), request.title(), request.content());

        return toResponse(post);
    }

    @PostMapping("/{postId}/comments")
    public CommentResponse createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        User author = findOrCreateLocalUser(request.author());
        Comment comment = Comment.create(post, author, request.content());
        Comment savedComment = commentRepository.save(comment);

        return toCommentResponse(savedComment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Transactional
    public void deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentRepository.deleteByIdAndPostId(commentId, postId);
    }

    @DeleteMapping("/{postId}")
    @Transactional
    public void deletePost(@PathVariable Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow();

        post.delete();
    }

    private User findOrCreateLocalUser(String authorName) {
        String localEmail = "local-" + Integer.toHexString(authorName.hashCode())
                + "@project-alpha.local";

        return userRepository.findByEmail(localEmail)
                .orElseGet(() -> userRepository.save(User.createLocalUser(authorName)));
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getName(),
                post.getCategory(),
                formatDateTime(post.getCreatedAt()),
                post.getTitle(),
                post.getContent(),
                List.of(),
                commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())
                        .stream()
                        .map(PostController::toCommentResponse)
                        .toList());
    }

    private static CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getName(),
                comment.getContent());
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

    public record UpdatePostRequest(
            String category,
            String title,
            String content,
            List<String> tags) {
    }

    public record CreateCommentRequest(
            String author,
            String content) {
    }
}
