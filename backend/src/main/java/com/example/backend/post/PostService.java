package com.example.backend.post;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import com.example.backend.tag.Tag;
import java.util.ArrayList;
import java.util.List;
import com.example.backend.tag.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    
    public Post createPost(PostCreateRequest request){
        List<Tag> tags = getTagsFromNames(request.getTagNames());

        Post post = new Post(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName(),
            LocalDateTime.now(),
            tags
        );
        
        return postRepository.save(post);
    }

    public Post updatePost(Long id, PostCreateRequest request){
        Post post = postRepository.findById(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        List<Tag> tags = getTagsFromNames(request.getTagNames());
        
        post.update(
            request.getTitle(),
            request.getContent(),
            request.getAuthorName(),
            tags
        );

        return postRepository.save(post);
    }

    public void deletePost(Long id){
        Post post = postRepository.findById(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        postRepository.delete(post);
    }

    public PostService(PostRepository postRepository, TagRepository tagRepository){
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
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
