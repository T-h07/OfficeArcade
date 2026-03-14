package com.officearcade.server.auth;

import com.officearcade.server.auth.dto.AuthenticatedUserResponse;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.identity.DevIdentityService;
import com.officearcade.server.identity.IdentityUser;
import com.officearcade.server.security.JwtService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final DevIdentityService devIdentityService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            DevIdentityService devIdentityService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.devIdentityService = devIdentityService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<IdentityUser> maybeUser = devIdentityService.findByEmail(request.email());
        if (maybeUser.isEmpty()) {
            throw invalidCredentials();
        }

        IdentityUser user = maybeUser.get();
        if (!user.enabled()) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                "Bearer",
                accessToken,
                toUserResponse(user)
        );
    }

    public AuthenticatedUserResponse toUserResponse(IdentityUser user) {
        return new AuthenticatedUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.role().name(),
                user.enabled()
        );
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }
}
