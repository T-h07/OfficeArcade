package com.officearcade.server.moderation;

import com.officearcade.server.moderation.dto.CreateModerationReportRequest;
import com.officearcade.server.moderation.dto.ModerationReportListResponse;
import com.officearcade.server.moderation.dto.ModerationReportResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationReportResponse createReport(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @Valid @RequestBody CreateModerationReportRequest request
    ) {
        return moderationService.createReport(getRequiredPrincipal(principal).id(), request);
    }

    @GetMapping("/me")
    public ModerationReportListResponse listMyReports(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return moderationService.listReportsForReporter(getRequiredPrincipal(principal).id());
    }

    private static OfficeArcadePrincipal getRequiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
