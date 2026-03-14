package com.officearcade.server.departments;

import com.officearcade.server.departments.dto.CreateDepartmentRequest;
import com.officearcade.server.departments.dto.DepartmentListResponse;
import com.officearcade.server.departments.dto.DepartmentResponse;
import com.officearcade.server.departments.dto.UpdateDepartmentRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {

    private final DepartmentManagementService departmentManagementService;

    public AdminDepartmentController(DepartmentManagementService departmentManagementService) {
        this.departmentManagementService = departmentManagementService;
    }

    @GetMapping
    public DepartmentListResponse listDepartments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return departmentManagementService.listDepartments(search, active);
    }

    @GetMapping("/{departmentId}")
    public DepartmentResponse getDepartment(@PathVariable String departmentId) {
        return departmentManagementService.getDepartment(departmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentManagementService.createDepartment(request);
    }

    @PutMapping("/{departmentId}")
    public DepartmentResponse updateDepartment(
            @PathVariable String departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return departmentManagementService.updateDepartment(departmentId, request);
    }

    @PostMapping("/{departmentId}/activate")
    public DepartmentResponse activateDepartment(@PathVariable String departmentId) {
        return departmentManagementService.setDepartmentActiveState(departmentId, true);
    }

    @PostMapping("/{departmentId}/deactivate")
    public DepartmentResponse deactivateDepartment(@PathVariable String departmentId) {
        return departmentManagementService.setDepartmentActiveState(departmentId, false);
    }
}
