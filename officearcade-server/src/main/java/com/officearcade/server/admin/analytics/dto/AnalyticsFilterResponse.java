package com.officearcade.server.admin.analytics.dto;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;

public record AnalyticsFilterResponse(
        String range,
        String rangeLabel,
        String departmentFilter,
        DepartmentSummaryResponse selectedDepartment,
        boolean unassignedOnly
) {
}
