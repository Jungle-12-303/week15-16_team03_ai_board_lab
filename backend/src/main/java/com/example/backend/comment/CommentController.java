package com.example.backend.comment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }

    @GetMapping("/api/posts/{postId}/comments")
    public List<Comment> getComments(@PathVariable Long postId) {
        return commentService.getComments(postId);
    }
   
    @PostMapping("/api/posts/{postId}/comments")
    public Comment createComment(
        @PathVariable Long postId,
        @RequestBody CommentCreateRequest request) {       
        return commentService.createComment(postId, request);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId){
        commentService.deleteComment(commentId);
    }

    @PutMapping("/api/comments/{commentId}")
    public Comment updateComment(@PathVariable Long commentId, @RequestBody CommentCreateRequest request) {
        return commentService.updateComment(commentId, request);
    }
    
}