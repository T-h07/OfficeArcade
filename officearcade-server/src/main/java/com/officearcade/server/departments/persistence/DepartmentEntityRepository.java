package com.officearcade.server.departments.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentEntityRepository extends JpaRepository<DepartmentEntity, UUID>, JpaSpecificationExecutor<DepartmentEntity> {

    Optional<DepartmentEntity> findById(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByDisplayNameIgnoreCase(String displayName);

    Optional<DepartmentEntity> findByCodeIgnoreCase(String code);

    Optional<DepartmentEntity> findByDisplayNameIgnoreCase(String displayName);

    List<DepartmentEntity> findAllByActiveTrueOrderByDisplayNameAsc();
}
