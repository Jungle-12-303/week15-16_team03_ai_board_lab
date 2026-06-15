package com.example.backend.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RagDatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(RagDatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiEmbeddingProperties properties;

    public RagDatabaseInitializer(
        JdbcTemplate jdbcTemplate,
        OpenAiEmbeddingProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (!ensureVectorExtension()) {
            log.warn("pgvector 확장을 사용할 수 없어 RAG 테이블 자동 준비를 건너뜁니다.");
            return;
        }

        int dimensions = properties.getEmbeddingDimensions();

        runDdl("""
            CREATE TABLE IF NOT EXISTS post_embeddings (
                id BIGSERIAL PRIMARY KEY,
                post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                chunk_index INTEGER NOT NULL DEFAULT 0,
                chunk_text TEXT NOT NULL,
                embedding_model VARCHAR(100) NOT NULL,
                embedding vector(%d) NOT NULL,
                created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                CONSTRAINT uq_post_embeddings_post_chunk_model
                    UNIQUE (post_id, chunk_index, embedding_model)
            )
            """.formatted(dimensions), "post_embeddings 테이블 생성");

        runDdl("""
            ALTER TABLE post_embeddings
                ALTER COLUMN embedding TYPE vector(%d)
                USING embedding::vector(%d)
            """.formatted(dimensions, dimensions), "embedding 차원 맞춤");

        runDdl("""
            CREATE INDEX IF NOT EXISTS idx_post_embeddings_post_id
                ON post_embeddings (post_id)
            """, "post_id 인덱스 생성");

        runDdl("""
            CREATE INDEX IF NOT EXISTS idx_post_embeddings_model
                ON post_embeddings (embedding_model)
            """, "embedding_model 인덱스 생성");

        runDdl("""
            CREATE INDEX IF NOT EXISTS idx_post_embeddings_embedding_hnsw
                ON post_embeddings
                USING hnsw (embedding vector_cosine_ops)
            """, "벡터 검색 인덱스 생성");

        log.info("RAG 저장소 준비 완료: embedding dimension={}", dimensions);
    }

    private boolean ensureVectorExtension() {
        Boolean installed = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')",
            Boolean.class
        );

        if (Boolean.TRUE.equals(installed)) {
            return true;
        }

        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception e) {
            log.warn("pgvector 확장 생성에 실패했습니다.", e);
            return false;
        }

        Boolean installedAfterCreate = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')",
            Boolean.class
        );

        return Boolean.TRUE.equals(installedAfterCreate);
    }

    private void runDdl(String sql, String description) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("{} 중 예외가 발생했습니다. 수동 점검이 필요할 수 있습니다.", description, e);
        }
    }
}
