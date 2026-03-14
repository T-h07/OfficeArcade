package com.officearcade.server.challenges;

import com.officearcade.server.challenges.dto.ChallengeListResponse;
import com.officearcade.server.challenges.dto.ChallengeDisputeRequest;
import com.officearcade.server.challenges.dto.ChallengeSummaryResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/challenges")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ChallengeController {

    private final PostMatchChallengeService postMatchChallengeService;

    public ChallengeController(PostMatchChallengeService postMatchChallengeService) {
        this.postMatchChallengeService = postMatchChallengeService;
    }

    @GetMapping("/me")
    public ChallengeListResponse listMyChallenges(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return postMatchChallengeService.listChallengesForUser(getRequiredPrincipal(principal).id());
    }

    @GetMapping("/{challengeId}")
    public ChallengeSummaryResponse getChallengeById(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String challengeId
    ) {
        return postMatchChallengeService.getChallengeForUser(getRequiredPrincipal(principal).id(), challengeId);
    }

    @PostMapping("/{challengeId}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public ChallengeSummaryResponse confirmChallenge(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String challengeId
    ) {
        return postMatchChallengeService.confirmChallenge(getRequiredPrincipal(principal).id(), challengeId);
    }

    @PostMapping("/{challengeId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public ChallengeSummaryResponse rejectChallenge(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String challengeId
    ) {
        return postMatchChallengeService.rejectChallenge(getRequiredPrincipal(principal).id(), challengeId);
    }

    @PostMapping("/{challengeId}/dispute")
    @ResponseStatus(HttpStatus.OK)
    public ChallengeSummaryResponse disputeChallenge(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String challengeId,
            @Valid @RequestBody(required = false) ChallengeDisputeRequest request
    ) {
        String note = request == null ? null : request.note();
        return postMatchChallengeService.disputeChallenge(getRequiredPrincipal(principal).id(), challengeId, note);
    }

    private static OfficeArcadePrincipal getRequiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
