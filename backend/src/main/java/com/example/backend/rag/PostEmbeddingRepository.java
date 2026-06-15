package com.example.backend.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;

@Repository
public class PostEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiEmbeddingProperties properties;

    public PostEmbeddingRepository(
        JdbcTemplate jdbcTemplate,
        OpenAiEmbeddingProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public void saveOrUpdate(Long postId, int chunkIndex, String chunkText, String embeddingModel, List<Double> embedding) {
        String sql = """
            INSERT INTO post_embeddings (
                post_id,
                chunk_index,
                chunk_text,
                embedding_model,
                embedding,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?::vector(%d), NOW(), NOW())
            ON CONFLICT (post_id, chunk_index, embedding_model)
            DO UPDATE SET
                chunk_text = EXCLUDED.chunk_text,
                embedding = EXCLUDED.embedding,
                updated_at = NOW()
            """.formatted(properties.getEmbeddingDimensions());

        jdbcTemplate.update(
            sql,
            postId,
            chunkIndex,
            chunkText,
            embeddingModel,
            toVectorLiteral(embedding)
        );
    }

    public void deleteByPostId(Long postId) {
        jdbcTemplate.update("DELETE FROM post_embeddings WHERE post_id = ?", postId);
    }

    public long countAllEmbeddings() {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM post_embeddings",
            Long.class
        );

        return count == null ? 0L : count;
    }

    public long countIndexedPosts() {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT post_id) FROM post_embeddings",
            Long.class
        );

        return count == null ? 0L : count;
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");

        for (Double value : embedding) {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }
}
