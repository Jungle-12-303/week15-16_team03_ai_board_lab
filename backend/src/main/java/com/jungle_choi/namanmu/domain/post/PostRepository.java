package com.jungle_choi.namanmu.domain.post;

import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    Optional<Post> findWithAuthorById(Long id);

    boolean existsByAuthor_EmailAndTitle(String authorEmail, String title);

    @Query("""
            select count(post) > 0
            from Post post
            where post.author.email = :authorEmail
              and post.content like concat('%', :sourceMarker, '%')
            """)
    boolean existsImportedSource(
            @Param("authorEmail") String authorEmail,
            @Param("sourceMarker") String sourceMarker);

    @EntityGraph(attributePaths = "author")
    @Query("""
            select post
            from Post post
            where post.status = :status
              and (:category = '' or :category = 'All' or post.category = :category)
              and (
                :tag = ''
                or exists (
                  select postTag.id
                  from PostTag postTag
                  where postTag.post = post
                    and lower(postTag.tag.name) like lower(concat('%', :tag, '%'))
                )
              )
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
            @Param("tag") String tag,
            Pageable pageable);

    @Query("""
            select post.category as category, count(post) as postCount
            from Post post
            where post.status = :status
              and (
                :tag = ''
                or exists (
                  select postTag.id
                  from PostTag postTag
                  where postTag.post = post
                    and lower(postTag.tag.name) like lower(concat('%', :tag, '%'))
                )
              )
              and (
                :keyword = ''
                or lower(post.title) like lower(concat('%', :keyword, '%'))
                or lower(post.content) like lower(concat('%', :keyword, '%'))
              )
            group by post.category
            """)
    List<CategoryCount> countByCategory(
            @Param("status") PostStatus status,
            @Param("keyword") String keyword,
            @Param("tag") String tag);

    @Query("""
            select post
            from Post post
            where post.status = :status
              and not exists (
                select chunk.id
                from PostEmbeddingChunk chunk
                where chunk.post = post
                  and chunk.embeddingModel = :embeddingModel
              )
              and not exists (
                select job.id
                from EmbeddingJob job
                where job.post = post
                  and job.status in :openJobStatuses
              )
            order by post.id asc
            """)
    List<Post> findPostsMissingEmbeddingChunks(
            @Param("status") PostStatus status,
            @Param("embeddingModel") String embeddingModel,
            @Param("openJobStatuses") List<EmbeddingJobStatus> openJobStatuses,
            Pageable pageable);

    interface CategoryCount {
        String getCategory();

        long getPostCount();
    }
}
