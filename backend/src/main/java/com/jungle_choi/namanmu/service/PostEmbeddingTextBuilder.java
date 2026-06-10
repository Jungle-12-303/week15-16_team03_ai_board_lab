package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostEmbeddingTextBuilder {

    private static final String DOCUMENT_CONTEXT = """
            This text is from Project Alpha, a board for development, learning, project, and daily logs.
            Use the category and tags as context for understanding the post topic.
            """;

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;

    public PostEmbeddingTextBuilder(
            PostRepository postRepository,
            PostTagRepository postTagRepository) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
    }

    @Transactional(readOnly = true)
    public String build(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow();

        return build(post);
    }

    public String build(Post post) {
        List<String> tags = postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();

        return build(post.getCategory(), post.getTitle(), post.getContent(), tags);
    }

    public String build(String category, String title, String content, List<String> tags) {
        return """
                Document Context:
                %s
                Category: %s
                Title: %s
                Content:
                %s

                Tags: %s
                """.formatted(
                DOCUMENT_CONTEXT.trim(),
                normalize(category),
                normalize(title),
                normalize(content),
                formatTags(tags));
    }

    private static String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "None";
        }

        String joinedTags = String.join(", ", tags.stream()
                .map(PostEmbeddingTextBuilder::normalize)
                .filter((tag) -> !tag.isBlank())
                .toList());

        if (joinedTags.isBlank()) {
            return "None";
        }

        return joinedTags;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }
}
