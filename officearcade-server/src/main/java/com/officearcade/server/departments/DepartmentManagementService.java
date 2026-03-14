package com.officearcade.server.departments;

import com.officearcade.server.departments.dto.CreateDepartmentRequest;
import com.officearcade.server.departments.dto.DepartmentListResponse;
import com.officearcade.server.departments.dto.DepartmentResponse;
import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.departments.dto.UpdateDepartmentRequest;
import com.officearcade.server.departments.persistence.DepartmentEntity;
import com.officearcade.server.departments.persistence.DepartmentEntityRepository;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepartmentManagementService {

    private final DepartmentEntityRepository departmentEntityRepository;
    private final UserEntityRepository userEntityRepository;

    public DepartmentManagementService(
            DepartmentEntityRepository departmentEntityRepository,
            UserEntityRepository userEntityRepository
    ) {
        this.departmentEntityRepository = departmentEntityRepository;
        this.userEntityRepository = userEntityRepository;
    }

    @Transactional(readOnly = true)
    public DepartmentListResponse listDepartments(String search, Boolean active) {
        Specification<DepartmentEntity> specification = Specification.where(null);
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            ));
        }
        if (active != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("active"), active));
        }

        List<DepartmentResponse> departments = departmentEntityRepository
                .findAll(specification, Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("code"))).stream()
                .map(this::toResponse)
                .toList();

        return new DepartmentListResponse(departments.size(), departments);
    }

    @Transactional(readOnly = true)
    public DepartmentListResponse listActiveDepartments() {
        List<DepartmentResponse> departments = departmentEntityRepository
                .findAllByActiveTrueOrderByDisplayNameAsc().stream()
                .map(this::toResponse)
                .toList();
        return new DepartmentListResponse(departments.size(), departments);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(String departmentIdText) {
        DepartmentEntity department = getRequiredDepartmentEntity(departmentIdText);
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String code = normalizeCode(request.code());
        String displayName = normalizeDisplayName(request.displayName());
        String description = normalizeDescription(request.description());

        if (departmentEntityRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists: " + code);
        }
        if (departmentEntityRepository.existsByDisplayNameIgnoreCase(displayName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department display name already exists: " + displayName
            );
        }

        DepartmentEntity department = new DepartmentEntity();
        department.setCode(code);
        department.setDisplayName(displayName);
        department.setDescription(description);
        department.setActive(true);

        DepartmentEntity saved = departmentEntityRepository.save(department);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(String departmentIdText, UpdateDepartmentRequest request) {
        DepartmentEntity department = getRequiredDepartmentEntity(departmentIdText);
        String code = normalizeCode(request.code());
        String displayName = normalizeDisplayName(request.displayName());
        String description = normalizeDescription(request.description());

        departmentEntityRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(department.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Department code already exists: " + code
                    );
                });

        departmentEntityRepository.findByDisplayNameIgnoreCase(displayName)
                .filter(existing -> !existing.getId().equals(department.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Department display name already exists: " + displayName
                    );
                });

        department.setCode(code);
        department.setDisplayName(displayName);
        department.setDescription(description);
        DepartmentEntity saved = departmentEntityRepository.save(department);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse setDepartmentActiveState(String departmentIdText, boolean active) {
        DepartmentEntity department = getRequiredDepartmentEntity(departmentIdText);
        department.setActive(active);
        DepartmentEntity saved = departmentEntityRepository.save(department);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentSummaryResponse> listDepartmentSummaries(boolean includeInactive) {
        List<DepartmentEntity> departments = includeInactive
                ? departmentEntityRepository.findAll(Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("code")))
                : departmentEntityRepository.findAllByActiveTrueOrderByDisplayNameAsc();
        return departments.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentEntity getRequiredDepartmentEntity(String departmentIdText) {
        UUID departmentId = parseDepartmentId(departmentIdText);
        return departmentEntityRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentIdText
                ));
    }

    @Transactional(readOnly = true)
    public DepartmentSummaryResponse toSummaryResponse(DepartmentEntity department) {
        return new DepartmentSummaryResponse(
                department.getId().toString(),
                department.getCode(),
                department.getDisplayName(),
                department.isActive()
        );
    }

    private DepartmentResponse toResponse(DepartmentEntity department) {
        int assignedUserCount = (int) userEntityRepository.countByDepartment_Id(department.getId());
        return new DepartmentResponse(
                department.getId().toString(),
                department.getCode(),
                department.getDisplayName(),
                department.getDescription(),
                department.isActive(),
                assignedUserCount,
                department.getCreatedAt().toString(),
                department.getUpdatedAt().toString()
        );
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department code is required.");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department code is required.");
        }
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40);
        }
        return normalized;
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department display name is required.");
        }
        String normalized = displayName.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department display name is required.");
        }
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 280) {
            return normalized.substring(0, 280);
        }
        return normalized;
    }

    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static UUID parseDepartmentId(String departmentIdText) {
        try {
            return UUID.fromString(departmentIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department id must be a valid UUID.");
        }
    }
}
