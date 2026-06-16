package com.jungle_choi.namanmu.domain.post;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @EntityGraph(attributePaths = "tag")
    List<PostTag> findAllByPostIdOrderByTagNameAsc(Long postId);

    @EntityGraph(attributePaths = {"post", "tag"})
    List<PostTag> findAllByPost_IdIn(Collection<Long> postIds);

    long deleteByPostId(Long postId);
}
