CREATE TABLE IF NOT EXISTS post_embeddings (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL DEFAULT 0,
    chunk_text TEXT NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding vector NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_post_embeddings_post_chunk_model
        UNIQUE (post_id, chunk_index, embedding_model)
);

CREATE INDEX IF NOT EXISTS idx_post_embeddings_post_id
    ON post_embeddings (post_id);

CREATE INDEX IF NOT EXISTS idx_post_embeddings_model
    ON post_embeddings (embedding_model);
