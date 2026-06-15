package com.jungle_choi.namanmu.service.post;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.rag.EmbeddingJobService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final EmbeddingJobService embeddingJobService;
    private final PostReadService postReadService;
    private final PostTagService postTagService;

    public PostService(
            PostRepository postRepository,
            EmbeddingJobService embeddingJobService,
            PostReadService postReadService,
            PostTagService postTagService) {
        this.postRepository = postRepository;
        this.embeddingJobService = embeddingJobService;
        this.postReadService = postReadService;
        this.postTagService = postTagService;
    }

    @Transactional(readOnly = true)
    public PostPageResult listPosts(
            String keyword,
            String category,
            String tag,
            int page,
            int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        String normalizedTag = normalize(tag);
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.search(
                PostStatus.PUBLISHED,
                normalizedKeyword,
                normalizedCategory,
                normalizedTag,
                pageRequest);

        return new PostPageResult(
                postPage.getContent(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                countCategories(normalizedKeyword, normalizedTag));
    }

    @Transactional
    public Post getPublishedPost(User user, Long postId) {
        Post post = postRepository.findWithAuthorById(postId)
                .filter((foundPost) -> foundPost.getStatus() == PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        postReadService.markRead(user, post);

        return post;
    }

    @Transactional
    public Post createPost(User author, SavePostCommand command) {
        Post post = Post.create(author, command.category(), command.title(), command.content());
        Post savedPost = postRepository.save(post);
        postTagService.updatePostTags(savedPost, command.tags());
        embeddingJobService.enqueuePostEmbedding(savedPost);

        return savedPost;
    }

    @Transactional
    public Post updatePost(User user, Long postId, SavePostCommand command) {
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.update(command.category(), command.title(), command.content());
        postTagService.updatePostTags(post, command.tags());
        embeddingJobService.enqueuePostEmbedding(post);

        return post;
    }

    @Transactional
    public void deletePost(User user, Long postId) {
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow();
        validatePostOwner(post, user);

        post.delete();
    }

    private List<CategoryCountResult> countCategories(String keyword, String tag) {
        return postRepository.countByCategory(PostStatus.PUBLISHED, keyword, tag)
                .stream()
                .map((categoryCount) -> new CategoryCountResult(
                        categoryCount.getCategory(),
                        categoryCount.getPostCount()))
                .toList();
    }

    private static void validatePostOwner(Post post, User user) {
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    public record SavePostCommand(
            String category,
            String title,
            String content,
            List<String> tags) {
    }

    public record PostPageResult(
            List<Post> posts,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<CategoryCountResult> categoryCounts) {
    }

    public record CategoryCountResult(String category, long count) {
    }
}
