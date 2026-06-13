package com.jungle_choi.namanmu.domain.read;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostReadRepository extends JpaRepository<PostRead, Long> {

    Optional<PostRead> findByUserIdAndPostId(Long userId, Long postId);

    @Query("""
            select post
            from PostRead postRead
              join postRead.post post
              join fetch post.author
            where postRead.user.id = :userId
              and post.status = :status
            order by postRead.readAt desc
            """)
    List<Post> findRecentReadPosts(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            Pageable pageable);
}
