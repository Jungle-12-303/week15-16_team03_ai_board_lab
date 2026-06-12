package com.jungle_choi.namanmu.api.mapper;

import com.jungle_choi.namanmu.api.dto.CommentResponse;
import com.jungle_choi.namanmu.api.dto.PostResponse;
import com.jungle_choi.namanmu.domain.comment.Comment;
import com.jungle_choi.namanmu.domain.comment.CommentRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class PostResponseMapper {

    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PostTagRepository postTagRepository;
    private final CommentRepository commentRepository;

    public PostResponseMapper(
            PostTagRepository postTagRepository,
            CommentRepository commentRepository) {
        this.postTagRepository = postTagRepository;
        this.commentRepository = commentRepository;
    }

    public PostResponse toPostResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getName(),
                post.getCategory(),
                formatDateTime(post.getCreatedAt()),
                post.getTitle(),
                post.getContent(),
                postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                        .stream()
                        .map((postTag) -> postTag.getTag().getName())
                        .toList(),
                commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())
                        .stream()
                        .map(this::toCommentResponse)
                        .toList());
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getName(),
                comment.getContent());
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        return dateTime.format(RESPONSE_DATE_FORMATTER);
    }
}
