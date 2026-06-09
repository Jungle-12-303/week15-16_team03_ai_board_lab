package com.jungle_choi.namanmu.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Post> findAllByStatus(PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
            PostStatus titleStatus,
            String titleKeyword,
            PostStatus contentStatus,
            String contentKeyword,
            Pageable pageable);
}
