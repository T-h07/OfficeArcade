package com.officearcade.server.departments.dto;

import java.util.List;

public record DepartmentListResponse(
        int total,
        List<DepartmentResponse> departments
) {
}
