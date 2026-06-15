package com.jungle_choi.namanmu.service.rag;

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
    private final PostChunkTextSplitter postChunkTextSplitter;

    public PostEmbeddingTextBuilder(
            PostRepository postRepository,
            PostTagRepository postTagRepository,
            PostChunkTextSplitter postChunkTextSplitter) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.postChunkTextSplitter = postChunkTextSplitter;
    }

    @Transactional(readOnly = true)
    public String build(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow();

        return build(post);
    }

    public String build(Post post) {
        List<String> tags = loadTags(post);

        return build(post.getCategory(), post.getTitle(), post.getContent(), tags);
    }

    @Transactional(readOnly = true)
    public List<ChunkSourceText> buildChunkSources(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow();
        List<String> tags = loadTags(post);

        return postChunkTextSplitter.split(post.getContent())
                .stream()
                .map((chunk) -> new ChunkSourceText(
                        chunk.chunkIndex(),
                        chunk.text(),
                        buildChunk(
                                post.getCategory(),
                                post.getTitle(),
                                chunk.chunkIndex(),
                                chunk.text(),
                                tags)))
                .toList();
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

    public String buildQuery(String category, String title, String content, List<String> tags) {
        return """
                Search Query:
                Category: %s
                Title: %s
                User draft or intent:
                %s

                Tags: %s
                """.formatted(
                normalizeQuery(category),
                normalizeQuery(title),
                normalizeQuery(content),
                formatQueryTags(tags));
    }

    private String buildChunk(
            String category,
            String title,
            int chunkIndex,
            String chunkText,
            List<String> tags) {
        return """
                Document Context:
                %s
                Category: %s
                Title: %s
                Chunk Index: %d
                Chunk Content:
                %s

                Tags: %s
                """.formatted(
                DOCUMENT_CONTEXT.trim(),
                normalize(category),
                normalize(title),
                chunkIndex,
                normalize(chunkText),
                formatTags(tags));
    }

    private List<String> loadTags(Post post) {
        return postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();
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

    private static String formatQueryTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "None";
        }

        String joinedTags = String.join(", ", tags.stream()
                .map(PostEmbeddingTextBuilder::normalizeQuery)
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

    private static String normalizeQuery(String text) {
        return normalize(text)
                .replace("깃허브", "GitHub")
                .replace("깃헙", "GitHub")
                .replace("깃랩", "GitLab")
                .replace("깃 액션", "GitHub Actions")
                .replace("스프링부트", "Spring Boot")
                .replace("스프링 부트", "Spring Boot")
                .replace("레디스", "Redis")
                .replace("리액트", "React")
                .replace("쿠버네티스", "Kubernetes")
                .replace("도커", "Docker");
    }

    public record ChunkSourceText(int chunkIndex, String chunkText, String sourceText) {
    }
}
