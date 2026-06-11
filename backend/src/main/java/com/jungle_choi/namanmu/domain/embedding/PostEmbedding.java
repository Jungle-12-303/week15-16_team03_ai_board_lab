package com.jungle_choi.namanmu.domain.embedding;

import com.jungle_choi.namanmu.domain.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_embeddings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_embeddings_post_id",
                        columnNames = "post_id")
        })
public class PostEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 100)
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

    public static PostEmbedding create(
            Post post,
            String embeddingModel,
            int dimensions,
            String embeddingJson,
            String sourceHash) {
        PostEmbedding postEmbedding = new PostEmbedding();
        postEmbedding.post = post;
        postEmbedding.embeddingModel = embeddingModel;
        postEmbedding.dimensions = dimensions;
        postEmbedding.embeddingJson = embeddingJson;
        postEmbedding.sourceHash = sourceHash;
        return postEmbedding;
    }

    public void replace(
            String embeddingModel,
            int dimensions,
            String embeddingJson,
            String sourceHash) {
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.embeddingJson = embeddingJson;
        this.sourceHash = sourceHash;
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
