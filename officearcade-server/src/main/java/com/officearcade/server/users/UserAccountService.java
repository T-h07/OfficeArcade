package com.officearcade.server.users;

import com.officearcade.server.departments.persistence.DepartmentEntity;
import com.officearcade.server.departments.persistence.DepartmentEntityRepository;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {

    private final PasswordEncoder passwordEncoder;
    private final UserEntityRepository userEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final DepartmentEntityRepository departmentEntityRepository;

    public UserAccountService(
            PasswordEncoder passwordEncoder,
            UserEntityRepository userEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            DepartmentEntityRepository departmentEntityRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userEntityRepository = userEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.departmentEntityRepository = departmentEntityRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(String id) {
        Optional<UUID> userId = tryParseUserId(id);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        return userEntityRepository.findById(userId.get()).map(this::toAccount);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userEntityRepository.findByEmail(normalizedEmail).map(this::toAccount);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findUsers(UserQuery query) {
        String normalizedSearch = normalizeSearch(query.search());

        Specification<UserEntity> specification = Specification.where(null);
        if (normalizedSearch != null) {
            String likePattern = "%" + normalizedSearch + "%";
            specification = specification.and((root, querySpec, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(root.get("email"), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), likePattern)
            ));
        }
        if (query.role() != null) {
            specification = specification.and((root, querySpec, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("role"), query.role()));
        }
        if (query.enabled() != null) {
            specification = specification.and((root, querySpec, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("enabled"), query.enabled()));
        }
        if (query.departmentFilter() != null) {
            if ("UNASSIGNED".equalsIgnoreCase(query.departmentFilter())) {
                specification = specification.and((root, querySpec, criteriaBuilder) ->
                        criteriaBuilder.isNull(root.get("department")));
            } else {
                UUID departmentId = parseRequiredDepartmentId(query.departmentFilter());
                specification = specification.and((root, querySpec, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("department").get("id"), departmentId));
            }
        }

        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("email"));
        return userEntityRepository.findAll(specification, sort).stream()
                .map(this::toAccount)
                .toList();
    }

    @Transactional
    public UserAccount create(CreateUserCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        if (userEntityRepository.findByEmail(normalizedEmail).isPresent()) {
            throw duplicateEmail(normalizedEmail);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(normalizedEmail);
        userEntity.setDisplayName(normalizeDisplayName(command.displayName()));
        userEntity.setPasswordHash(passwordEncoder.encode(command.rawPassword()));
        userEntity.setRole(command.role());
        userEntity.setEnabled(command.enabled());

        UserEntity saved = userEntityRepository.save(userEntity);
        ensurePlayerProfile(saved);
        return toAccount(saved);
    }

    @Transactional
    public UserAccount update(UpdateUserCommand command) {
        UserAccount existing = getRequiredUser(command.id());
        String nextEmail = normalizeEmail(command.email());
        String currentEmail = existing.email();

        if (!currentEmail.equals(nextEmail)) {
            Optional<UserAccount> existingForEmail = findByEmail(nextEmail);
            if (existingForEmail.isPresent() && !existingForEmail.get().id().equals(existing.id())) {
                throw duplicateEmail(nextEmail);
            }
        }

        assertNotRemovingLastActiveAdmin(existing, command.role(), existing.enabled());

        UserEntity userEntity = getRequiredUserEntity(existing.id());
        userEntity.setEmail(nextEmail);
        userEntity.setDisplayName(normalizeDisplayName(command.displayName()));
        userEntity.setRole(command.role());
        UserEntity saved = userEntityRepository.save(userEntity);
        return toAccount(saved);
    }

    @Transactional
    public UserAccount setEnabled(String id, boolean enabled) {
        UserAccount existing = getRequiredUser(id);
        assertNotRemovingLastActiveAdmin(existing, existing.role(), enabled);

        UserEntity userEntity = getRequiredUserEntity(existing.id());
        userEntity.setEnabled(enabled);
        UserEntity saved = userEntityRepository.save(userEntity);
        return toAccount(saved);
    }

    @Transactional
    public UserAccount resetPassword(String id, String rawPassword) {
        UserEntity userEntity = getRequiredUserEntity(id);
        userEntity.setPasswordHash(passwordEncoder.encode(rawPassword));
        UserEntity saved = userEntityRepository.save(userEntity);
        return toAccount(saved);
    }

    @Transactional
    public UserAccount assignDepartment(String userIdText, String departmentIdText) {
        UserEntity userEntity = getRequiredUserEntity(userIdText);

        if (departmentIdText == null || departmentIdText.trim().isEmpty()) {
            userEntity.setDepartment(null);
            return toAccount(userEntityRepository.save(userEntity));
        }

        UUID departmentId = parseRequiredDepartmentId(departmentIdText);
        DepartmentEntity department = departmentEntityRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentIdText
                ));

        userEntity.setDepartment(department);
        return toAccount(userEntityRepository.save(userEntity));
    }

    public boolean passwordMatches(UserAccount user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.passwordHash());
    }

    private UserAccount getRequiredUser(String id) {
        return findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private UserEntity getRequiredUserEntity(String id) {
        UUID userId = parseRequiredUserId(id);
        return userEntityRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private void assertNotRemovingLastActiveAdmin(UserAccount current, AppRole nextRole, boolean nextEnabled) {
        boolean currentIsActiveAdmin = current.role() == AppRole.ADMIN && current.enabled();
        boolean remainsActiveAdmin = nextRole == AppRole.ADMIN && nextEnabled;

        if (!currentIsActiveAdmin || remainsActiveAdmin) {
            return;
        }

        long activeAdminCount = userEntityRepository.countByRoleAndEnabledTrue(AppRole.ADMIN);

        if (activeAdminCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot remove or deactivate the last active ADMIN account."
            );
        }
    }

    private void ensurePlayerProfile(UserEntity userEntity) {
        if (playerProfileEntityRepository.existsByUserId(userEntity.getId())) {
            return;
        }

        PlayerProfileEntity playerProfileEntity = new PlayerProfileEntity();
        playerProfileEntity.setUser(userEntity);
        playerProfileEntity.setLevel(1);
        playerProfileEntity.setXp(0);
        playerProfileEntity.setRespectPoints(0);
        playerProfileEntity.setKarmaPoints(0);
        playerProfileEntity.setGamesPlayed(0);
        playerProfileEntity.setWins(0);
        playerProfileEntity.setLosses(0);
        playerProfileEntityRepository.save(playerProfileEntity);
    }

    private UserAccount toAccount(UserEntity userEntity) {
        DepartmentEntity department = userEntity.getDepartment();
        return new UserAccount(
                userEntity.getId().toString(),
                userEntity.getEmail(),
                userEntity.getDisplayName(),
                userEntity.getPasswordHash(),
                userEntity.getRole(),
                userEntity.isEnabled(),
                userEntity.isSuspended(),
                userEntity.getSuspendedAt(),
                userEntity.getSuspensionNote(),
                userEntity.getSuspendedByAdminId() == null ? null : userEntity.getSuspendedByAdminId().toString(),
                department == null ? null : department.getId().toString(),
                department == null ? null : department.getCode(),
                department == null ? null : department.getDisplayName(),
                department == null ? null : department.isActive(),
                userEntity.getCreatedAt(),
                userEntity.getUpdatedAt()
        );
    }

    private static ResponseStatusException duplicateEmail(String normalizedEmail) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A user with this email already exists: " + normalizedEmail
        );
    }

    private static UUID parseRequiredUserId(String id) {
        Optional<UUID> parsed = tryParseUserId(id);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id must be a valid UUID.");
    }

    private static UUID parseRequiredDepartmentId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department id must be a valid UUID.");
        }
    }

    private static Optional<UUID> tryParseUserId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        String normalized = email.toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        return normalized;
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required.");
        }
        String normalized = displayName.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required.");
        }
        return normalized;
    }

    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String value = search.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }
}
