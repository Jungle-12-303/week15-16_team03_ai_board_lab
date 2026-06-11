package com.jungle_choi.namanmu.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    long countByAuthor_Email(String email);

    @EntityGraph(attributePaths = "author")
    @Query("""
            select post
            from Post post
            where post.status = :status
              and (:category = '' or :category = 'All' or post.category = :category)
              and (
                :keyword = ''
                or lower(post.title) like lower(concat('%', :keyword, '%'))
                or lower(post.content) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Post> search(
            @Param("status") PostStatus status,
            @Param("keyword") String keyword,
            @Param("category") String category,
            Pageable pageable);
}
