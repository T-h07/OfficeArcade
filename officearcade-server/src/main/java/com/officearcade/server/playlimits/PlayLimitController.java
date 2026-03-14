package com.officearcade.server.playlimits;

import com.officearcade.server.playlimits.dto.PlayLimitSummaryResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/play-limits")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class PlayLimitController {

    private final PlayLimitService playLimitService;

    public PlayLimitController(PlayLimitService playLimitService) {
        this.playLimitService = playLimitService;
    }

    @GetMapping("/me")
    public PlayLimitSummaryResponse getMyPlayLimits(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return playLimitService.getSummaryForUser(requiredPrincipal(principal).id());
    }

    @GetMapping("/me/eligibility")
    public PlayLimitSummaryResponse getMyPlayEligibility(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return playLimitService.getSummaryForUser(requiredPrincipal(principal).id());
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
