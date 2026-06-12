package com.jungle_choi.namanmu.domain.read;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReadRepository extends JpaRepository<PostRead, Long> {

    Optional<PostRead> findByUserIdAndPostId(Long userId, Long postId);
}
