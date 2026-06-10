package com.example.backend.comment;

import com.example.backend.post.Post;
import com.example.backend.post.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public List<Comment> getComments(Long postId){
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ResponseStatusException(
                HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        return commentRepository.findByPost(post);
    }

    public Comment createComment(Long postId, CommentCreateRequest request){
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        Comment comment = new Comment(
            request.getContent(),
            request.getAuthorName(),
            LocalDateTime.now(),
            post
        );

        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(()-> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        commentRepository.delete(comment);
    }

    public Comment updateComment(Long commentId, CommentCreateRequest request){
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(()->new ResponseStatusException(
                HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        comment.update(request.getContent());

        return commentRepository.save(comment);
    }
}   
