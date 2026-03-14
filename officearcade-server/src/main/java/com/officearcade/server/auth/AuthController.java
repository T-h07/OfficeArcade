package com.officearcade.server.auth;

import com.officearcade.server.auth.dto.AuthenticatedUserResponse;
import com.officearcade.server.auth.dto.CurrentUserResponse;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.auth.dto.RoleCheckResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserAccountService userAccountService;

    public AuthController(AuthService authService, UserAccountService userAccountService) {
        this.authService = authService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }

        Optional<UserAccount> maybeUser = userAccountService.findById(principal.id());
        if (maybeUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer available.");
        }

        AuthenticatedUserResponse userResponse = authService.toUserResponse(maybeUser.get());
        return new CurrentUserResponse(userResponse);
    }

    @GetMapping("/role-check/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleCheckResponse adminRoleCheck(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }

        return new RoleCheckResponse("OK", "ADMIN_ONLY", principal.role().name());
    }
}
