package com.officearcade.server.identity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DevIdentityService {

    private final Map<String, IdentityUser> usersByEmail;
    private final Map<String, IdentityUser> usersById;

    public DevIdentityService(SeededUserProperties properties) {
        this.usersByEmail = new LinkedHashMap<>();
        this.usersById = new LinkedHashMap<>();

        for (SeededUserProperties.SeededUser seededUser : properties.seededUsers()) {
            IdentityUser identityUser = new IdentityUser(
                    seededUser.id(),
                    normalizeEmail(seededUser.email()),
                    seededUser.displayName(),
                    seededUser.passwordHash(),
                    seededUser.role(),
                    seededUser.enabled()
            );

            String normalizedEmail = identityUser.email();
            if (usersByEmail.containsKey(normalizedEmail)) {
                throw new IllegalStateException("Duplicate seeded user email: " + normalizedEmail);
            }
            if (usersById.containsKey(identityUser.id())) {
                throw new IllegalStateException("Duplicate seeded user id: " + identityUser.id());
            }

            usersByEmail.put(normalizedEmail, identityUser);
            usersById.put(identityUser.id(), identityUser);
        }
    }

    public Optional<IdentityUser> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(normalizeEmail(email)));
    }

    public Optional<IdentityUser> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
