ALTER TABLE post_embeddings
    ALTER COLUMN embedding TYPE vector(1536)
    USING embedding::vector(1536);

CREATE INDEX IF NOT EXISTS idx_post_embeddings_embedding_hnsw
    ON post_embeddings
    USING hnsw (embedding vector_cosine_ops);
