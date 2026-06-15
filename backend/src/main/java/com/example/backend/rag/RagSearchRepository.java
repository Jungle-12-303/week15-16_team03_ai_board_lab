package com.example.backend.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;

@Repository
public class RagSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiEmbeddingProperties properties;

    public RagSearchRepository(
        JdbcTemplate jdbcTemplate,
        OpenAiEmbeddingProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public List<RagSearchMatch> searchSimilarPosts(
        List<Double> embedding,
        String embeddingModel,
        int limit
    ) {
        String sql = """
            WITH ranked AS (
                SELECT DISTINCT ON (pe.post_id)
                    pe.post_id,
                    pe.chunk_text,
                    pe.embedding <=> ?::vector(%d) AS distance
                FROM post_embeddings pe
                WHERE pe.embedding_model = ?
                ORDER BY pe.post_id, pe.embedding <=> ?::vector(%d)
            )
            SELECT
                ranked.post_id,
                ranked.chunk_text,
                ranked.distance
            FROM ranked
            ORDER BY ranked.distance
            LIMIT ?
            """.formatted(
            properties.getEmbeddingDimensions(),
            properties.getEmbeddingDimensions()
        );

        String vectorLiteral = toVectorLiteral(embedding);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new RagSearchMatch(
                rs.getLong("post_id"),
                rs.getString("chunk_text"),
                rs.getDouble("distance")
            ),
            vectorLiteral,
            embeddingModel,
            vectorLiteral,
            limit
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");

        for (Double value : embedding) {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }
}
