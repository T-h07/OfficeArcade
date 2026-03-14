package com.officearcade.server.moderation;

import com.officearcade.server.challenges.dto.ChallengeSummaryResponse;
import com.officearcade.server.moderation.dto.AdminChallengeResolutionRequest;
import com.officearcade.server.moderation.dto.AdminChallengeReviewListResponse;
import com.officearcade.server.moderation.dto.AdminDismissReportRequest;
import com.officearcade.server.moderation.dto.AdminReportActionRequest;
import com.officearcade.server.moderation.dto.ChallengePolicyListResponse;
import com.officearcade.server.moderation.dto.ChallengePolicyUpdateRequest;
import com.officearcade.server.moderation.dto.ModerationAuditListResponse;
import com.officearcade.server.moderation.dto.ModerationReportListResponse;
import com.officearcade.server.moderation.dto.ModerationReportResponse;
import com.officearcade.server.moderation.dto.ModerationUserStateResponse;
import com.officearcade.server.moderation.dto.AdminSuspendUserRequest;
import com.officearcade.server.security.OfficeArcadePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final ModerationService moderationService;

    public AdminModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/reports")
    public ModerationReportListResponse listReports(
            @RequestParam(required = false) ModerationReportStatus status,
            @RequestParam(required = false) ModerationReportCategory category
    ) {
        return moderationService.listReportsForAdmin(status, category);
    }

    @GetMapping("/reports/{reportId}")
    public ModerationReportResponse getReportById(@PathVariable String reportId) {
        return moderationService.getReportForAdmin(reportId);
    }

    @PostMapping("/reports/{reportId}/dismiss")
    public ModerationReportResponse dismissReport(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String reportId,
            @Valid @RequestBody(required = false) AdminDismissReportRequest request
    ) {
        AdminDismissReportRequest resolvedRequest = request == null ? new AdminDismissReportRequest(null) : request;
        return moderationService.dismissReport(getRequiredPrincipal(principal).id(), reportId, resolvedRequest);
    }

    @PostMapping("/reports/{reportId}/take-action")
    public ModerationReportResponse takeActionOnReport(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String reportId,
            @Valid @RequestBody AdminReportActionRequest request
    ) {
        return moderationService.takeReportAction(getRequiredPrincipal(principal).id(), reportId, request);
    }

    @GetMapping("/challenges")
    public AdminChallengeReviewListResponse listDisputedChallenges() {
        return moderationService.listDisputedChallenges();
    }

    @PostMapping("/challenges/{challengeId}/resolve")
    public ChallengeSummaryResponse resolveChallengeDispute(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String challengeId,
            @Valid @RequestBody AdminChallengeResolutionRequest request
    ) {
        return moderationService.resolveDisputedChallenge(getRequiredPrincipal(principal).id(), challengeId, request);
    }

    @PostMapping("/users/{userId}/suspend")
    @ResponseStatus(HttpStatus.OK)
    public ModerationUserStateResponse suspendUser(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody(required = false) AdminSuspendUserRequest request
    ) {
        String note = request == null ? null : request.note();
        return moderationService.suspendUser(getRequiredPrincipal(principal).id(), userId, note);
    }

    @PostMapping("/users/{userId}/unsuspend")
    @ResponseStatus(HttpStatus.OK)
    public ModerationUserStateResponse unsuspendUser(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody(required = false) AdminSuspendUserRequest request
    ) {
        String note = request == null ? null : request.note();
        return moderationService.unsuspendUser(getRequiredPrincipal(principal).id(), userId, note);
    }

    @GetMapping("/audit")
    public ModerationAuditListResponse listAuditTrail() {
        return moderationService.listAuditEntries();
    }

    @GetMapping("/policies")
    public ChallengePolicyListResponse listChallengePolicies() {
        return moderationService.listChallengePolicies();
    }

    @PutMapping("/policies")
    public ChallengePolicyListResponse updateChallengePolicies(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @Valid @RequestBody ChallengePolicyUpdateRequest request
    ) {
        return moderationService.updateChallengePolicies(getRequiredPrincipal(principal).id(), request);
    }

    private static OfficeArcadePrincipal getRequiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
