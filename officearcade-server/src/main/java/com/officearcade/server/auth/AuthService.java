package com.officearcade.server.auth;

import com.officearcade.server.auth.dto.AuthenticatedUserResponse;
import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.security.JwtService;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountService userAccountService;
    private final JwtService jwtService;

    public AuthService(
            UserAccountService userAccountService,
            JwtService jwtService
    ) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<UserAccount> maybeUser = userAccountService.findByEmail(request.email());
        if (maybeUser.isEmpty()) {
            throw invalidCredentials();
        }

        UserAccount user = maybeUser.get();
        if (!user.enabled()) {
            throw invalidCredentials();
        }

        if (!userAccountService.passwordMatches(user, request.password())) {
            throw invalidCredentials();
        }

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                "Bearer",
                accessToken,
                toUserResponse(user)
        );
    }

    public AuthenticatedUserResponse toUserResponse(UserAccount user) {
        return new AuthenticatedUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.role().name(),
                user.enabled(),
                user.suspended(),
                nullableInstant(user.suspendedAt()),
                user.suspensionNote(),
                toDepartmentSummary(user)
        );
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }

    private static String nullableInstant(java.time.Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static DepartmentSummaryResponse toDepartmentSummary(UserAccount user) {
        if (user.departmentId() == null) {
            return null;
        }
        return new DepartmentSummaryResponse(
                user.departmentId(),
                user.departmentCode(),
                user.departmentDisplayName(),
                Boolean.TRUE.equals(user.departmentActive())
        );
    }
}
