package com.example.backend.post;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestHeader;

@CrossOrigin(origins = "*")
@RestController
public class PostController {
    
    private final PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    @GetMapping("/api/posts")
    public Page<PostResponse> getPosts(
        @RequestParam(required = false) String keyword,
        @PageableDefault(
            sort = {"createdAt", "id"},
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return postService.getPosts(keyword, pageable);
    }
    
    @GetMapping("/api/posts/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }
    
    @PostMapping("/api/posts")
    public PostResponse createPost(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody PostCreateRequest request
    ) {
        return postService.createPost(authorizationHeader, request);
    }

    @PutMapping("/api/posts/{id}")
    public PostResponse updatePost(
        @RequestHeader("Authorization") String authorizationHeader,
        @PathVariable Long id,
        @RequestBody PostCreateRequest request
    ) {
        return postService.updatePost(authorizationHeader, id, request);
    }
    
    @DeleteMapping("/api/posts/{id}")
    public void deletePost(
        @RequestHeader("Authorization") String authorizationHeader,
        @PathVariable Long id
    ){
        postService.deletePost(authorizationHeader, id);
    }
}
