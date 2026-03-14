package com.officearcade.server.admin.users;

import com.officearcade.server.admin.users.dto.AdminUserActionResponse;
import com.officearcade.server.admin.users.dto.AdminUserListResponse;
import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.AssignUserDepartmentRequest;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.admin.users.dto.ResetUserPasswordRequest;
import com.officearcade.server.admin.users.dto.UpdateAdminUserRequest;
import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.users.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public AdminUserListResponse listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AppRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String departmentId
    ) {
        List<AdminUserResponse> users = adminUserManagementService.listUsers(search, role, active, departmentId).stream()
                .map(AdminUserManagementController::toResponse)
                .toList();

        return new AdminUserListResponse(users, users.size());
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUserById(@PathVariable String id) {
        return toResponse(adminUserManagementService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        UserAccount created = adminUserManagementService.createUser(
                request.email(),
                request.displayName(),
                request.password(),
                request.role(),
                request.enabled()
        );
        return toResponse(created);
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateAdminUserRequest request
    ) {
        UserAccount updated = adminUserManagementService.updateUser(
                id,
                request.email(),
                request.displayName(),
                request.role()
        );
        return toResponse(updated);
    }

    @PostMapping("/{id}/activate")
    public AdminUserResponse activateUser(@PathVariable String id) {
        return toResponse(adminUserManagementService.activateUser(id));
    }

    @PostMapping("/{id}/deactivate")
    public AdminUserResponse deactivateUser(@PathVariable String id) {
        return toResponse(adminUserManagementService.deactivateUser(id));
    }

    @PostMapping("/{id}/reset-password")
    public AdminUserActionResponse resetPassword(
            @PathVariable String id,
            @Valid @RequestBody ResetUserPasswordRequest request
    ) {
        adminUserManagementService.resetPassword(id, request.newPassword());
        return new AdminUserActionResponse("OK", "Password reset successfully.");
    }

    @PostMapping("/{id}/assign-department")
    public AdminUserResponse assignDepartment(
            @PathVariable String id,
            @RequestBody(required = false) AssignUserDepartmentRequest request
    ) {
        String departmentId = request == null ? null : request.departmentId();
        return toResponse(adminUserManagementService.assignDepartment(id, departmentId));
    }

    private static AdminUserResponse toResponse(UserAccount user) {
        return new AdminUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.role().name(),
                user.enabled(),
                toDepartmentSummary(user),
                user.createdAt().toString(),
                user.updatedAt().toString()
        );
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
