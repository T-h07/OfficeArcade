package com.officearcade.server.employee.dashboard;

import com.officearcade.server.employee.dashboard.dto.EmployeeDashboardResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employee/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class EmployeeDashboardController {

    private final EmployeeDashboardService employeeDashboardService;

    public EmployeeDashboardController(EmployeeDashboardService employeeDashboardService) {
        this.employeeDashboardService = employeeDashboardService;
    }

    @GetMapping
    public EmployeeDashboardResponse getDashboard(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }

        return employeeDashboardService.getDashboardForUser(principal.id());
    }
}
