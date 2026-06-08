package com.jungle_choi.namanmu.domain.post;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @EntityGraph(attributePaths = "tag")
    List<PostTag> findAllByPostIdOrderByTagNameAsc(Long postId);

    long deleteByPostId(Long postId);
}
