package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.comment.Comment;
import com.jungle_choi.namanmu.domain.comment.CommentRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Comment createComment(User author, Long postId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        Comment comment = Comment.create(post, author, content);

        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(User user, Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        validateCommentOwner(comment, postId, user);
        commentRepository.delete(comment);
    }

    private static void validateCommentOwner(Comment comment, Long postId, User user) {
        boolean samePost = comment.getPost().getId().equals(postId);
        boolean sameAuthor = comment.getAuthor().getId().equals(user.getId());

        if (!samePost || !sameAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
