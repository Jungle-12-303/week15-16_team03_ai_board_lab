package com.example.backend.post;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;

    public Post createPost(PostCreateRequest request){
        Post post = new Post(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName(),
            LocalDateTime.now()
        );
        
        return postRepository.save(post);
    }

    public Post updatePost(Long id, PostCreateRequest request){
        Post post = postRepository.findById(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        post.update(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName()
        );

        return postRepository.save(post);
    }

    public void deletePost(Long id){
        Post post = postRepository.findById(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        postRepository.delete(post);
    }

    public PostService(PostRepository postRepository){
        this.postRepository = postRepository;
    }

    public List<Post> getPosts(){
        return postRepository.findAll();
    }

    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

}
