package com.example.aiknowledgeboard.comment;

import com.example.aiknowledgeboard.auth.CurrentUserService;
import com.example.aiknowledgeboard.post.Post;
import com.example.aiknowledgeboard.post.PostRepository;
import com.example.aiknowledgeboard.user.UserEntity;
import com.example.aiknowledgeboard.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CurrentUserService currentUserService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, CurrentUserService currentUserService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public CommentResponse create(Long postId, CommentRequest request) {
        UserEntity user = currentUserService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
        Comment comment = commentRepository.save(new Comment(post, user, request.content().trim()));
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long commentId) {
        UserEntity user = currentUserService.getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("댓글 작성자만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }
}

