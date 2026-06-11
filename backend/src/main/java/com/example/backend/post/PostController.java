package com.example.backend.post;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class PostController {
    
    private final PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    @GetMapping("/api/posts")
    public Page<Post> getPosts(
        @RequestParam(required = false) String keyword,
        Pageable pageable
    ) {
        return postService.getPosts(keyword, pageable);
    }
    
    @GetMapping("/api/posts/{id}")
    public Post getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }
    
    @PostMapping("/api/posts")
    public Post creaPost(@RequestBody PostCreateRequest request) {
        return postService.createPost(request);
    }

    @PutMapping("/api/posts/{id}")
    public Post updatePost(@PathVariable Long id, @RequestBody PostCreateRequest request) {
        return postService.updatePost(id, request);
    }
    
    @DeleteMapping("/api/posts/{id}")
    public void deletePost(@PathVariable Long id){
        postService.deletePost(id);
    }
}
