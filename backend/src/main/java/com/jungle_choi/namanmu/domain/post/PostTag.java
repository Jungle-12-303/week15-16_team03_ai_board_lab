package com.jungle_choi.namanmu.domain.post;

import com.jungle_choi.namanmu.domain.tag.Tag;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_tags_post_id_tag_id",
                        columnNames = {"post_id", "tag_id"})
        })
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    private LocalDateTime createdAt;

    public static PostTag create(Post post, Tag tag) {
        PostTag postTag = new PostTag();
        postTag.post = post;
        postTag.tag = tag;
        return postTag;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Tag getTag() {
        return tag;
    }
}
