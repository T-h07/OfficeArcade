package com.officearcade.server.users.persistence;

import com.officearcade.server.identity.AppRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserEntityRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmail(String email);

    long countByRoleAndEnabledTrue(AppRole role);

    List<UserEntity> findAllByEnabledTrueOrderByCreatedAtAscIdAsc();
}
