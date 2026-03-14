package com.officearcade.server.leaderboards;

import com.officearcade.server.leaderboards.dto.LeaderboardEntryResponse;
import com.officearcade.server.leaderboards.dto.LeaderboardResponse;
import com.officearcade.server.leaderboards.dto.LeaderboardTypeListResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/leaderboards")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/types")
    public LeaderboardTypeListResponse getTypes() {
        return leaderboardService.getAvailableTypes();
    }

    @GetMapping
    public LeaderboardResponse getLeaderboard(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @RequestParam(name = "type", defaultValue = "WINS") LeaderboardType type,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "departmentId", required = false) String departmentId
    ) {
        return leaderboardService.getLeaderboard(resolvePrincipalId(principal), type, limit, departmentId);
    }

    @GetMapping("/{type}")
    public LeaderboardResponse getLeaderboardByPath(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable LeaderboardType type,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "departmentId", required = false) String departmentId
    ) {
        return leaderboardService.getLeaderboard(resolvePrincipalId(principal), type, limit, departmentId);
    }

    @GetMapping("/{type}/me")
    public LeaderboardEntryResponse getCurrentUserRank(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable LeaderboardType type
    ) {
        return leaderboardService.getCurrentUserRank(resolvePrincipalId(principal), type);
    }

    private static String resolvePrincipalId(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal.id();
    }
}
