package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.domain.comment.Comment;
import com.jungle_choi.namanmu.domain.comment.CommentRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.tag.Tag;
import com.jungle_choi.namanmu.domain.tag.TagRepository;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.EmbeddingJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PostController {

    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final EmbeddingJobService embeddingJobService;

    public PostController(
            PostRepository postRepository,
            CommentRepository commentRepository,
            TagRepository tagRepository,
            PostTagRepository postTagRepository,
            EmbeddingJobService embeddingJobService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
        this.embeddingJobService = embeddingJobService;
    }

    @GetMapping
    public PostPageResponse listPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "All") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String normalizedKeyword = keyword.trim();
        String normalizedCategory = category.trim();
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = searchPosts(normalizedKeyword, normalizedCategory, pageRequest);

        return new PostPageResponse(
                postPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                countCategories(normalizedKeyword));
    }

    private Page<Post> searchPosts(String keyword, String category, PageRequest pageRequest) {
        return postRepository.search(
                PostStatus.PUBLISHED,
                keyword,
                category,
                pageRequest);
    }

    private List<CategoryCountResponse> countCategories(String keyword) {
        return postRepository.countByCategory(PostStatus.PUBLISHED, keyword)
                .stream()
                .map((categoryCount) -> new CategoryCountResponse(
                        categoryCount.getCategory(),
                        categoryCount.getPostCount()))
                .toList();
    }

    private static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    @PostMapping
    @Transactional
    public PostResponse createPost(
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreatePostRequest request) {
        Post post = Post.create(author, request.category(), request.title(), request.content());
        Post savedPost = postRepository.save(post);
        updatePostTags(savedPost, request.tags());
        embeddingJobService.enqueuePostEmbedding(savedPost);

        return toResponse(savedPost);
    }

    @PatchMapping("/{postId}")
    @Transactional
    public PostResponse updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.update(request.category(), request.title(), request.content());
        updatePostTags(post, request.tags());
        embeddingJobService.enqueuePostEmbedding(post);

        return toResponse(post);
    }

    @PostMapping("/{postId}/comments")
    public CommentResponse createComment(
            @AuthenticationPrincipal User author,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        Comment comment = Comment.create(post, author, request.content());
        Comment savedComment = commentRepository.save(comment);

        return toCommentResponse(savedComment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Transactional
    public void deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        validateCommentOwner(comment, postId, user);
        commentRepository.delete(comment);
    }

    @DeleteMapping("/{postId}")
    @Transactional
    public void deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.delete();
    }

    private static void validatePostOwner(Post post, User user) {
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static void validateCommentOwner(Comment comment, Long postId, User user) {
        boolean samePost = comment.getPost().getId().equals(postId);
        boolean sameAuthor = comment.getAuthor().getId().equals(user.getId());

        if (!samePost || !sameAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void updatePostTags(Post post, List<String> tagNames) {
        postTagRepository.deleteByPostId(post.getId());
        postTagRepository.flush();

        normalizeTags(tagNames).forEach((tagName) -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
            postTagRepository.save(PostTag.create(post, tag));
        });
    }

    private static Set<String> normalizeTags(List<String> tagNames) {
        Set<String> normalizedTags = new LinkedHashSet<>();
        Set<String> normalizedTagKeys = new HashSet<>();

        if (tagNames == null) {
            return normalizedTags;
        }

        tagNames.stream()
                .map(String::trim)
                .filter((tagName) -> !tagName.isBlank())
                .forEach((tagName) -> {
                    String tagKey = tagName.toLowerCase(Locale.ROOT);
                    if (normalizedTagKeys.add(tagKey)) {
                        normalizedTags.add(tagName);
                    }
                });

        return normalizedTags;
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getName(),
                post.getCategory(),
                formatDateTime(post.getCreatedAt()),
                post.getTitle(),
                post.getContent(),
                postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                        .stream()
                        .map((postTag) -> postTag.getTag().getName())
                        .toList(),
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

    public record PostPageResponse(
            List<PostResponse> posts,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<CategoryCountResponse> categoryCounts) {
    }

    public record CategoryCountResponse(String category, long count) {
    }

    public record CommentResponse(Long id, String author, String content) {
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

    public record CreateCommentRequest(
            @NotBlank(message = "comment content is required.")
            @Size(max = 1000, message = "comment content must be 1000 characters or fewer.")
            String content) {
    }
}
