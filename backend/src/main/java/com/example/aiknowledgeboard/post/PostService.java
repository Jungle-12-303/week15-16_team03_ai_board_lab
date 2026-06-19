package com.example.aiknowledgeboard.post;

import com.example.aiknowledgeboard.ai.rag2.RagService2;
import com.example.aiknowledgeboard.auth.CurrentUserService;
import com.example.aiknowledgeboard.comment.CommentRepository;
import com.example.aiknowledgeboard.comment.CommentService;
import com.example.aiknowledgeboard.common.PageResponse;
import com.example.aiknowledgeboard.tag.Tag;
import com.example.aiknowledgeboard.tag.TagService;
import com.example.aiknowledgeboard.user.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentService commentService;
    private final TagService tagService;
    private final CurrentUserService currentUserService;
    private final RagService2 ragService;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            CommentService commentService,
            TagService tagService,
            CurrentUserService currentUserService,
            RagService2 ragService
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentService = commentService;
        this.tagService = tagService;
        this.currentUserService = currentUserService;
        this.ragService = ragService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> list(int page, int size, String keyword, String tag) {
        String normalizedKeyword = normalizeNullable(keyword);
        String normalizedTag = normalizeNullable(tag);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = findPosts(normalizedKeyword, normalizedTag, pageable);
        return PageResponse.from(posts.map(post -> PostSummaryResponse.from(post, tagService, commentRepository.countByPostId(post.getId()))));
    }

    @Transactional(readOnly = true)
    public PostDetailResponse get(Long id) {
        Post post = findPost(id);
        return toDetail(post);
    }

    @Transactional
    public PostDetailResponse create(PostRequest request) {
        UserEntity user = currentUserService.getCurrentUser();
        Set<Tag> tags = tagService.getOrCreateTags(request.tags());
        Post post = postRepository.save(new Post(user, request.title().trim(), request.content().trim(), tags));
        indexPostSafely(post);
        return toDetail(post);
    }

    @Transactional
    public PostDetailResponse update(Long id, PostRequest request) {
        UserEntity user = currentUserService.getCurrentUser();
        Post post = findPost(id);
        verifyOwner(post, user);
        Set<Tag> tags = tagService.getOrCreateTags(request.tags());
        post.update(request.title().trim(), request.content().trim(), tags);
        indexPostSafely(post);
        return toDetail(post);
    }

    @Transactional
    public void delete(Long id) {
        UserEntity user = currentUserService.getCurrentUser();
        Post post = findPost(id);
        verifyOwner(post, user);
        deleteIndexSafely(post);
        postRepository.delete(post);
    }

    @Transactional
    public int reindexAllForRag() {
        List<Post> posts = postRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return ragService.reindexAll(posts);
    }

    private PostDetailResponse toDetail(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                tagService.toNames(post.getTags()),
                commentService.findByPost(post.getId()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
    }

    private void verifyOwner(Post post, UserEntity user) {
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("게시글 작성자만 변경할 수 있습니다.");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private Page<Post> findPosts(String keyword, String tag, Pageable pageable) {
        if (keyword == null && tag == null) {
            return postRepository.findAll(pageable);
        }
        if (keyword == null) {
            return postRepository.searchByTag(tag, pageable);
        }
        String keywordPattern = "%" + keyword + "%";
        if (tag == null) {
            return postRepository.searchByKeyword(keywordPattern, pageable);
        }
        return postRepository.searchByKeywordAndTag(keywordPattern, tag, pageable);
    }

    private void indexPostSafely(Post post) {
        try {
            ragService.loadIndex(post);
        } catch (Exception ignored) {
            // Posting must remain available even when an external AI or vector DB call fails.
        }
    }

    private void deleteIndexSafely(Post post) {
        try {
            ragService.delete(post);
        } catch (Exception ignored) {
            // Deleting a post must not fail just because the vector index cleanup failed.
        }
    }
}
