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
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.security.JwtTokenService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final JwtTokenService jwtTokenService;

    public PostController(
            PostRepository postRepository,
            UserRepository userRepository,
            CommentRepository commentRepository,
            TagRepository tagRepository,
            PostTagRepository postTagRepository,
            JwtTokenService jwtTokenService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping
    public PostPageResponse listPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "All") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = searchPosts(keyword, category, pageRequest);

        return new PostPageResponse(
                postPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages());
    }

    private Page<Post> searchPosts(String keyword, String category, PageRequest pageRequest) {
        String normalizedKeyword = keyword.trim();
        String normalizedCategory = category.trim();

        if (normalizedKeyword.isBlank()) {
            if (normalizedCategory.isBlank() || normalizedCategory.equals("All")) {
                return postRepository.findAllByStatus(PostStatus.PUBLISHED, pageRequest);
            }

            return postRepository.findAllByStatusAndCategory(
                    PostStatus.PUBLISHED,
                    normalizedCategory,
                    pageRequest);
        }

        return postRepository.findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
                PostStatus.PUBLISHED,
                normalizedKeyword,
                PostStatus.PUBLISHED,
                normalizedKeyword,
                pageRequest);
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
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody CreatePostRequest request) {
        User author = findAuthenticatedUser(authorizationHeader);
        Post post = Post.create(author, request.category(), request.title(), request.content());
        Post savedPost = postRepository.save(post);
        updatePostTags(savedPost, request.tags());

        return toResponse(savedPost);
    }

    @PatchMapping("/{postId}")
    @Transactional
    public PostResponse updatePost(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long postId,
            @RequestBody UpdatePostRequest request) {
        User user = findAuthenticatedUser(authorizationHeader);
        Post post = postRepository.findById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.update(request.category(), request.title(), request.content());
        updatePostTags(post, request.tags());

        return toResponse(post);
    }

    @PostMapping("/{postId}/comments")
    public CommentResponse createComment(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        User author = findAuthenticatedUser(authorizationHeader);
        Comment comment = Comment.create(post, author, request.content());
        Comment savedComment = commentRepository.save(comment);

        return toCommentResponse(savedComment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Transactional
    public void deleteComment(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        User user = findAuthenticatedUser(authorizationHeader);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        validateCommentOwner(comment, postId, user);
        commentRepository.delete(comment);
    }

    @DeleteMapping("/{postId}")
    @Transactional
    public void deletePost(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long postId) {
        User user = findAuthenticatedUser(authorizationHeader);
        Post post = postRepository.findById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.delete();
    }

    private User findAuthenticatedUser(String authorizationHeader) {
        String email = jwtTokenService.readEmailFromAuthorizationHeader(authorizationHeader);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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

        if (tagNames == null) {
            return normalizedTags;
        }

        tagNames.stream()
                .map(String::trim)
                .filter((tagName) -> !tagName.isBlank())
                .forEach(normalizedTags::add);

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
            int totalPages) {
    }

    public record CommentResponse(Long id, String author, String content) {
    }

    public record CreatePostRequest(
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
            String content) {
    }
}
