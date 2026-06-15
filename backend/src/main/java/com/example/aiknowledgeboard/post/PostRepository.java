package com.example.aiknowledgeboard.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("""
            select distinct p
            from Post p
            left join p.tags t
            where lower(p.title) like :keywordPattern
               or lower(p.content) like :keywordPattern
            """)
    Page<Post> searchByKeyword(@Param("keywordPattern") String keywordPattern, Pageable pageable);

    @Query("""
            select distinct p
            from Post p
            join p.tags t
            where t.name = :tag
            """)
    Page<Post> searchByTag(@Param("tag") String tag, Pageable pageable);

    @Query("""
            select distinct p
            from Post p
            join p.tags t
            where (lower(p.title) like :keywordPattern
                or lower(p.content) like :keywordPattern)
              and t.name = :tag
            """)
    Page<Post> searchByKeywordAndTag(
            @Param("keywordPattern") String keywordPattern,
            @Param("tag") String tag,
            Pageable pageable
    );
}
