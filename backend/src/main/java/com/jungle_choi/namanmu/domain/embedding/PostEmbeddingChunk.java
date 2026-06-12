package com.jungle_choi.namanmu.domain.embedding;

import com.jungle_choi.namanmu.domain.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_embedding_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_embedding_chunks_post_chunk",
                        columnNames = {"post_id", "chunk_index"})
        },
        indexes = {
                @Index(
                        name = "idx_post_embedding_chunks_embedding_model",
                        columnList = "embedding_model")
        })
public class PostEmbeddingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String chunkText;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(nullable = false)
    private int dimensions;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String embeddingJson;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static PostEmbeddingChunk create(
            Post post,
            int chunkIndex,
            String chunkText,
            String embeddingModel,
            int dimensions,
            String embeddingJson,
            String sourceHash) {
        PostEmbeddingChunk chunk = new PostEmbeddingChunk();
        chunk.post = post;
        chunk.chunkIndex = chunkIndex;
        chunk.chunkText = chunkText;
        chunk.embeddingModel = embeddingModel;
        chunk.dimensions = dimensions;
        chunk.embeddingJson = embeddingJson;
        chunk.sourceHash = sourceHash;
        return chunk;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public String getSourceHash() {
        return sourceHash;
    }
}
