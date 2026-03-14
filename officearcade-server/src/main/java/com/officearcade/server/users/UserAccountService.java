package com.officearcade.server.users;

import com.officearcade.server.identity.AppRole;
import com.officearcade.server.identity.SeededUserProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserAccount> usersById;
    private final Map<String, String> userIdByNormalizedEmail;

    public UserAccountService(SeededUserProperties seededUserProperties, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.usersById = new LinkedHashMap<>();
        this.userIdByNormalizedEmail = new LinkedHashMap<>();

        Instant seededAt = Instant.now();
        for (SeededUserProperties.SeededUser seededUser : seededUserProperties.seededUsers()) {
            String normalizedEmail = normalizeEmail(seededUser.email());
            if (userIdByNormalizedEmail.containsKey(normalizedEmail)) {
                throw new IllegalStateException("Duplicate seeded user email: " + normalizedEmail);
            }
            if (usersById.containsKey(seededUser.id())) {
                throw new IllegalStateException("Duplicate seeded user id: " + seededUser.id());
            }

            UserAccount account = new UserAccount(
                    seededUser.id(),
                    normalizedEmail,
                    seededUser.displayName().trim(),
                    seededUser.passwordHash(),
                    seededUser.role(),
                    seededUser.enabled(),
                    seededAt,
                    seededAt
            );
            usersById.put(account.id(), account);
            userIdByNormalizedEmail.put(normalizedEmail, account.id());
        }
    }

    public synchronized Optional<UserAccount> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public synchronized Optional<UserAccount> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String id = userIdByNormalizedEmail.get(normalizedEmail);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(id));
    }

    public synchronized List<UserAccount> findUsers(UserQuery query) {
        String normalizedSearch = normalizeSearch(query.search());
        List<UserAccount> users = new ArrayList<>(usersById.values());

        return users.stream()
                .filter((user) -> matchesSearch(user, normalizedSearch))
                .filter((user) -> query.role() == null || user.role() == query.role())
                .filter((user) -> query.enabled() == null || user.enabled() == query.enabled())
                .sorted(Comparator.comparing(UserAccount::createdAt).thenComparing(UserAccount::email))
                .toList();
    }

    public synchronized UserAccount create(CreateUserCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        if (userIdByNormalizedEmail.containsKey(normalizedEmail)) {
            throw duplicateEmail(normalizedEmail);
        }

        Instant now = Instant.now();
        String id = "user-" + UUID.randomUUID();
        UserAccount user = new UserAccount(
                id,
                normalizedEmail,
                normalizeDisplayName(command.displayName()),
                passwordEncoder.encode(command.rawPassword()),
                command.role(),
                command.enabled(),
                now,
                now
        );

        usersById.put(id, user);
        userIdByNormalizedEmail.put(normalizedEmail, id);
        return user;
    }

    public synchronized UserAccount update(UpdateUserCommand command) {
        UserAccount existing = getRequiredUser(command.id());
        String nextEmail = normalizeEmail(command.email());
        String currentEmail = existing.email();

        if (!currentEmail.equals(nextEmail)) {
            String existingIdForEmail = userIdByNormalizedEmail.get(nextEmail);
            if (existingIdForEmail != null && !existingIdForEmail.equals(existing.id())) {
                throw duplicateEmail(nextEmail);
            }
        }

        assertNotRemovingLastActiveAdmin(existing, command.role(), existing.enabled());

        UserAccount updated = new UserAccount(
                existing.id(),
                nextEmail,
                normalizeDisplayName(command.displayName()),
                existing.passwordHash(),
                command.role(),
                existing.enabled(),
                existing.createdAt(),
                Instant.now()
        );

        usersById.put(updated.id(), updated);
        if (!currentEmail.equals(nextEmail)) {
            userIdByNormalizedEmail.remove(currentEmail);
            userIdByNormalizedEmail.put(nextEmail, updated.id());
        }

        return updated;
    }

    public synchronized UserAccount setEnabled(String id, boolean enabled) {
        UserAccount existing = getRequiredUser(id);
        assertNotRemovingLastActiveAdmin(existing, existing.role(), enabled);

        UserAccount updated = new UserAccount(
                existing.id(),
                existing.email(),
                existing.displayName(),
                existing.passwordHash(),
                existing.role(),
                enabled,
                existing.createdAt(),
                Instant.now()
        );
        usersById.put(updated.id(), updated);
        return updated;
    }

    public synchronized UserAccount resetPassword(String id, String rawPassword) {
        UserAccount existing = getRequiredUser(id);
        UserAccount updated = new UserAccount(
                existing.id(),
                existing.email(),
                existing.displayName(),
                passwordEncoder.encode(rawPassword),
                existing.role(),
                existing.enabled(),
                existing.createdAt(),
                Instant.now()
        );
        usersById.put(updated.id(), updated);
        return updated;
    }

    public boolean passwordMatches(UserAccount user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.passwordHash());
    }

    private UserAccount getRequiredUser(String id) {
        UserAccount user = usersById.get(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        return user;
    }

    private void assertNotRemovingLastActiveAdmin(UserAccount current, AppRole nextRole, boolean nextEnabled) {
        boolean currentIsActiveAdmin = current.role() == AppRole.ADMIN && current.enabled();
        boolean remainsActiveAdmin = nextRole == AppRole.ADMIN && nextEnabled;

        if (!currentIsActiveAdmin || remainsActiveAdmin) {
            return;
        }

        long activeAdminCount = usersById.values().stream()
                .filter((user) -> user.role() == AppRole.ADMIN && user.enabled())
                .count();

        if (activeAdminCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot remove or deactivate the last active ADMIN account."
            );
        }
    }

    private static ResponseStatusException duplicateEmail(String normalizedEmail) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A user with this email already exists: " + normalizedEmail
        );
    }

    private static boolean matchesSearch(UserAccount user, String normalizedSearch) {
        if (normalizedSearch == null) {
            return true;
        }
        return user.email().contains(normalizedSearch)
                || user.displayName().toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            return "";
        }
        return displayName.trim();
    }

    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String value = search.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }
}
