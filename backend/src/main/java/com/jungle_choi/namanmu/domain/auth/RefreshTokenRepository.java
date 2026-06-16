package com.jungle_choi.namanmu.domain.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken refreshToken
            set refreshToken.revokedAt = :now,
                refreshToken.updatedAt = :now
            where refreshToken.user.id = :userId
                and refreshToken.revokedAt is null
                and refreshToken.expiresAt > :now
            """)
    int revokeActiveTokensByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}
