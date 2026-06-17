package com.example.backend.post;

import com.example.backend.rag.PostEmbeddingService;
import com.example.backend.tag.Tag;
import com.example.backend.tag.TagRepository;
import com.example.backend.user.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PostEmbeddingService postEmbeddingService;

    public PostService(
        PostRepository postRepository,
        TagRepository tagRepository,
        JwtTokenProvider jwtTokenProvider,
        PostEmbeddingService postEmbeddingService
    ) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.postEmbeddingService = postEmbeddingService;
    }

    public PostResponse createPost(String authorizationHeader, PostCreateRequest request) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);
        List<Tag> tags = getTagsFromNames(request.getTagNames());

        Post post = new Post(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName(),
            loginId,
            LocalDateTime.now(),
            tags
        );

        Post savedPost = postRepository.save(post);
        syncEmbeddingSafely(savedPost);
        return PostResponse.from(savedPost);
    }

    public PostResponse updatePost(String authorizationHeader, Long id, PostCreateRequest request) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);

        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!post.getOwnerLoginId().equals(loginId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "자기 글만 수정할 수 있습니다.");
        }

        List<Tag> tags = getTagsFromNames(request.getTagNames());

        post.update(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName(),
            tags
        );

        Post savedPost = postRepository.save(post);
        syncEmbeddingSafely(savedPost);
        return PostResponse.from(savedPost);
    }

    public void deletePost(String authorizationHeader, Long id) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);

        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!post.getOwnerLoginId().equals(loginId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "자기 글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
        postEmbeddingService.deletePostEmbeddings(id);
    }

    public Page<PostResponse> getPosts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return postRepository.findAll(pageable).map(PostResponse::from);
        }

        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable)
            .map(PostResponse::from);
    }

    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        return PostResponse.from(post);
    }

    private List<Tag> getTagsFromNames(List<String> tagNames) {
        List<Tag> tags = new ArrayList<>();

        if (tagNames == null) {
            return tags;
        }

        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(new Tag(tagName)));

            tags.add(tag);
        }

        return tags;
    }

    private void syncEmbeddingSafely(Post post) {
        try {
            postEmbeddingService.syncPostEmbedding(post);
        } catch (Exception e) {
            log.warn("게시글 {} 임베딩 동기화에 실패했습니다.", post.getId(), e);
        }
    }
}
