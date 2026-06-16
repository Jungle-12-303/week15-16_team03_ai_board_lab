package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.api.dto.CommentResponse;
import com.jungle_choi.namanmu.api.mapper.PostResponseMapper;
import com.jungle_choi.namanmu.domain.comment.Comment;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.post.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final PostResponseMapper postResponseMapper;

    public CommentController(
            CommentService commentService,
            PostResponseMapper postResponseMapper) {
        this.commentService = commentService;
        this.postResponseMapper = postResponseMapper;
    }

    @PostMapping
    public CommentResponse createComment(
            @AuthenticationPrincipal User author,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentService.createComment(author, postId, request.content());

        return postResponseMapper.toCommentResponse(comment);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.deleteComment(user, postId, commentId);
    }

    public record CreateCommentRequest(
            @NotBlank(message = "comment content is required.")
            @Size(max = 1000, message = "comment content must be 1000 characters or fewer.")
            String content) {
    }
}
