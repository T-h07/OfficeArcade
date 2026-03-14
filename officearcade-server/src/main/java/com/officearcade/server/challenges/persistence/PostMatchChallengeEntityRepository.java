package com.officearcade.server.challenges.persistence;

import com.officearcade.server.challenges.ChallengeStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostMatchChallengeEntityRepository extends JpaRepository<PostMatchChallengeEntity, UUID> {

    Optional<PostMatchChallengeEntity> findBySourceGameSession_Id(UUID sourceGameSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PostMatchChallengeEntity c where c.id = :challengeId")
    Optional<PostMatchChallengeEntity> findByIdForUpdate(@Param("challengeId") UUID challengeId);

    @Query("""
            select c
            from PostMatchChallengeEntity c
            where c.obligatedUser.id = :userId
               or c.beneficiaryUser.id = :userId
            order by c.createdAt desc
            """)
    List<PostMatchChallengeEntity> findAllForUser(@Param("userId") UUID userId);

    @Query("""
            select count(c)
            from PostMatchChallengeEntity c
            where c.status = :status
              and (c.obligatedUser.id = :userId or c.beneficiaryUser.id = :userId)
            """)
    long countByStatusForUser(@Param("status") ChallengeStatus status, @Param("userId") UUID userId);
}
