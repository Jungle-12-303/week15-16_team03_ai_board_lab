package com.jungle_choi.namanmu.domain.read;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.user.User;
import jakarta.persistence.Column;
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
        name = "post_reads",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_reads_user_id_post_id",
                        columnNames = {"user_id", "post_id"})
        })
public class PostRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostRead create(User user, Post post) {
        PostRead postRead = new PostRead();
        postRead.user = user;
        postRead.post = post;
        postRead.readAt = LocalDateTime.now();
        return postRead;
    }

    public void markAgain() {
        this.readAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
