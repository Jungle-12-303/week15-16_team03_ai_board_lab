package com.example.backend.post;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import com.example.backend.tag.Tag;
import java.util.ArrayList;
import java.util.List;
import com.example.backend.tag.TagRepository;
import com.example.backend.user.JwtTokenProvider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public Post createPost(String authorizationHeader, PostCreateRequest request){  
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

        return postRepository.save(post);
    }

    public Post updatePost(String authorizationHeader, Long id, PostCreateRequest request){
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

        return postRepository.save(post);
    }

    public void deletePost(String authorizationHeader, Long id){
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
    }

    public PostService(PostRepository postRepository, TagRepository tagRepository, JwtTokenProvider jwtTokenProvider){
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Page<Post> getPosts(String keyword, Pageable pageable){
        if (keyword == null || keyword.isBlank()) {
            return postRepository.findAll(pageable);
        }

        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
    }

    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private List<Tag> getTagsFromNames(List<String>tagNames){
        List<Tag> tags = new ArrayList<>();

        if(tagNames == null){
            return tags;
        }

        for(String tagName : tagNames){
            Tag tag = tagRepository.findByName(tagName)
                .orElseGet(()->tagRepository.save(new Tag(tagName)));

            tags.add(tag);
        }

        return tags;
    }

}
