package com.officearcade.server.admin.users;

import com.officearcade.server.identity.AppRole;
import com.officearcade.server.users.CreateUserCommand;
import com.officearcade.server.users.UpdateUserCommand;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import com.officearcade.server.users.UserQuery;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserManagementService {

    private final UserAccountService userAccountService;

    public AdminUserManagementService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public List<UserAccount> listUsers(String search, AppRole role, Boolean active, String departmentFilter) {
        return userAccountService.findUsers(new UserQuery(search, role, active, departmentFilter));
    }

    public UserAccount getUser(String id) {
        return userAccountService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    public UserAccount createUser(String email, String displayName, String rawPassword, AppRole role, Boolean enabled) {
        AppRole resolvedRole = role == null ? AppRole.EMPLOYEE : role;
        boolean resolvedEnabled = enabled == null || enabled;
        return userAccountService.create(
                new CreateUserCommand(email, displayName, rawPassword, resolvedRole, resolvedEnabled)
        );
    }

    public UserAccount updateUser(String id, String email, String displayName, AppRole role) {
        return userAccountService.update(new UpdateUserCommand(id, email, displayName, role));
    }

    public UserAccount activateUser(String id) {
        return userAccountService.setEnabled(id, true);
    }

    public UserAccount deactivateUser(String id) {
        return userAccountService.setEnabled(id, false);
    }

    public UserAccount resetPassword(String id, String rawPassword) {
        return userAccountService.resetPassword(id, rawPassword);
    }

    public UserAccount assignDepartment(String id, String departmentId) {
        return userAccountService.assignDepartment(id, departmentId);
    }
}
