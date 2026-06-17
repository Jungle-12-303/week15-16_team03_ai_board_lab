package com.example.backend.comment;

import com.example.backend.post.Post;
import com.example.backend.post.PostRepository;
import com.example.backend.user.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public CommentService(
        CommentRepository commentRepository,
        PostRepository postRepository,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public List<CommentResponse> getComments(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "게시글을 찾을 수 없습니다."
            ));

        return commentRepository.findByPost(post).stream()
            .map(CommentResponse::from)
            .toList();
    }

    public CommentResponse createComment(String authorizationHeader, Long postId, CommentCreateRequest request) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "게시글을 찾을 수 없습니다."
            ));

        Comment comment = new Comment(
            request.getContent(),
            request.getAuthorName(),
            loginId,
            LocalDateTime.now(),
            post
        );

        return CommentResponse.from(commentRepository.save(comment));
    }

    public void deleteComment(String authorizationHeader, Long commentId) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "댓글을 찾을 수 없습니다."
            ));

        if (!comment.getOwnerLoginId().equals(loginId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "자기 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    public CommentResponse updateComment(
        String authorizationHeader,
        Long commentId,
        CommentCreateRequest request
    ) {
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(token);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "댓글을 찾을 수 없습니다."
            ));

        if (!comment.getOwnerLoginId().equals(loginId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "자기 댓글만 수정할 수 있습니다.");
        }

        comment.update(request.getContent());

        return CommentResponse.from(commentRepository.save(comment));
    }
}
