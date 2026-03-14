package com.officearcade.server.departments;

import com.officearcade.server.departments.dto.DepartmentListResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class DepartmentDirectoryController {

    private final DepartmentManagementService departmentManagementService;

    public DepartmentDirectoryController(DepartmentManagementService departmentManagementService) {
        this.departmentManagementService = departmentManagementService;
    }

    @GetMapping
    public DepartmentListResponse listDepartments(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {
        if (activeOnly) {
            return departmentManagementService.listActiveDepartments();
        }
        return departmentManagementService.listDepartments(null, null);
    }
}
