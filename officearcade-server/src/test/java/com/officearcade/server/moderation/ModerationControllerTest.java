package com.officearcade.server.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.CurrentUserResponse;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.challenges.dto.ChallengeDisputeRequest;
import com.officearcade.server.challenges.dto.ChallengeSummaryResponse;
import com.officearcade.server.employee.dashboard.dto.EmployeeDashboardResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourGameStateResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourMoveRequest;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import com.officearcade.server.moderation.dto.AdminChallengeResolutionRequest;
import com.officearcade.server.moderation.dto.AdminChallengeReviewListResponse;
import com.officearcade.server.moderation.dto.AdminReportActionRequest;
import com.officearcade.server.moderation.dto.AdminSuspendUserRequest;
import com.officearcade.server.moderation.dto.CreateModerationReportRequest;
import com.officearcade.server.moderation.dto.ModerationAuditListResponse;
import com.officearcade.server.moderation.dto.ModerationReportListResponse;
import com.officearcade.server.moderation.dto.ModerationReportResponse;
import com.officearcade.server.moderation.dto.ModerationUserStateResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModerationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateReportsSuspendAndUnsuspendWithAuditTrail() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        TestUser reporter = createEmployeeUser(adminToken, "reporter");
        TestUser target = createEmployeeUser(adminToken, "target");

        String reporterToken = loginAndGetToken(reporter.email(), reporter.password());
        String targetToken = loginAndGetToken(target.email(), target.password());

        ResponseEntity<ModerationReportResponse> createReportResponse = restTemplate.exchange(
                baseUrl("/api/reports"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateModerationReportRequest(
                                target.userId(),
                                ModerationReportCategory.UNSPORTSMANLIKE_BEHAVIOR,
                                "Intentional griefing in room.",
                                null,
                                null,
                                null
                        ),
                        authHeaders(reporterToken)
                ),
                ModerationReportResponse.class
        );
        assertThat(createReportResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createReportResponse.getBody()).isNotNull();
        assertThat(createReportResponse.getBody().status()).isEqualTo("OPEN");
        String reportId = createReportResponse.getBody().id();

        ResponseEntity<ModerationReportListResponse> queueResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/reports"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                ModerationReportListResponse.class
        );
        assertThat(queueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queueResponse.getBody()).isNotNull();
        assertThat(queueResponse.getBody().reports()).extracting(ModerationReportResponse::id).contains(reportId);

        ResponseEntity<ModerationReportResponse> detailResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/reports/" + reportId),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                ModerationReportResponse.class
        );
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).isNotNull();
        assertThat(detailResponse.getBody().status()).isEqualTo("IN_REVIEW");

        ResponseEntity<ModerationReportResponse> noteOnlyResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/reports/" + reportId + "/take-action"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new AdminReportActionRequest(ModerationReportActionType.NOTE_ONLY, "Internal note only."),
                        authHeaders(adminToken)
                ),
                ModerationReportResponse.class
        );
        assertThat(noteOnlyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noteOnlyResponse.getBody()).isNotNull();
        assertThat(noteOnlyResponse.getBody().status()).isEqualTo("RESOLVED");

        ResponseEntity<ModerationUserStateResponse> suspendResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/users/" + target.userId() + "/suspend"),
                HttpMethod.POST,
                new HttpEntity<>(new AdminSuspendUserRequest("Temporary suspension"), authHeaders(adminToken)),
                ModerationUserStateResponse.class
        );
        assertThat(suspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(suspendResponse.getBody()).isNotNull();
        assertThat(suspendResponse.getBody().suspended()).isTrue();

        ResponseEntity<CurrentUserResponse> suspendedMeResponse = restTemplate.exchange(
                baseUrl("/api/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                CurrentUserResponse.class
        );
        assertThat(suspendedMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(suspendedMeResponse.getBody()).isNotNull();
        assertThat(suspendedMeResponse.getBody().user().suspended()).isTrue();

        ResponseEntity<String> suspendedDashboardResponse = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                String.class
        );
        assertThat(suspendedDashboardResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ModerationUserStateResponse> unsuspendResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/users/" + target.userId() + "/unsuspend"),
                HttpMethod.POST,
                new HttpEntity<>(new AdminSuspendUserRequest("Restore access"), authHeaders(adminToken)),
                ModerationUserStateResponse.class
        );
        assertThat(unsuspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unsuspendResponse.getBody()).isNotNull();
        assertThat(unsuspendResponse.getBody().suspended()).isFalse();

        ResponseEntity<EmployeeDashboardResponse> dashboardAfterUnsuspend = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(dashboardAfterUnsuspend.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ModerationAuditListResponse> auditResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/audit"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                ModerationAuditListResponse.class
        );
        assertThat(auditResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditResponse.getBody()).isNotNull();
        assertThat(auditResponse.getBody().entries()).extracting(entry -> entry.actionType())
                .contains("REPORT_NOTE_ONLY", "USER_SUSPENDED", "USER_UNSUSPENDED");
    }

    @Test
    void shouldMoveChallengeToDisputedStateAndResolveWithoutDoubleApplyingPoints() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        TestUser winner = createEmployeeUser(adminToken, "winner");
        TestUser loser = createEmployeeUser(adminToken, "loser");

        String winnerToken = loginAndGetToken(winner.email(), winner.password());
        String loserToken = loginAndGetToken(loser.email(), loser.password());

        leaveMyRoomIfPresent(winnerToken);
        leaveMyRoomIfPresent(loserToken);

        FinishedMatch finishedMatch = createAndFinishWinningMatch(winnerToken, loserToken);

        ResponseEntity<EmployeeDashboardResponse> loserDashboardBefore = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loserToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(loserDashboardBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loserDashboardBefore.getBody()).isNotNull();
        int respectBefore = loserDashboardBefore.getBody().respectPoints();
        int karmaBefore = loserDashboardBefore.getBody().karmaPoints();

        ResponseEntity<ChallengeSummaryResponse> disputeResponse = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/dispute"),
                HttpMethod.POST,
                new HttpEntity<>(new ChallengeDisputeRequest("Challenge needs admin review."), authHeaders(loserToken)),
                ChallengeSummaryResponse.class
        );
        assertThat(disputeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disputeResponse.getBody()).isNotNull();
        assertThat(disputeResponse.getBody().status()).isEqualTo("DISPUTED");

        ResponseEntity<String> beneficiaryConfirmWhileDisputed = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/confirm"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                String.class
        );
        assertThat(beneficiaryConfirmWhileDisputed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<AdminChallengeReviewListResponse> disputedListResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/challenges"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                AdminChallengeReviewListResponse.class
        );
        assertThat(disputedListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disputedListResponse.getBody()).isNotNull();
        assertThat(disputedListResponse.getBody().challenges()).extracting(challenge -> challenge.challengeId())
                .contains(finishedMatch.challengeId());

        ResponseEntity<ChallengeSummaryResponse> resolveResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/challenges/" + finishedMatch.challengeId() + "/resolve"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new AdminChallengeResolutionRequest(
                                ChallengeDisputeResolutionType.CANCEL_WITHOUT_POINTS,
                                "No points should be applied."
                        ),
                        authHeaders(adminToken)
                ),
                ChallengeSummaryResponse.class
        );
        assertThat(resolveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resolveResponse.getBody()).isNotNull();
        assertThat(resolveResponse.getBody().status()).isEqualTo("CANCELLED");
        assertThat(resolveResponse.getBody().respectPointsAwarded()).isEqualTo(0);
        assertThat(resolveResponse.getBody().karmaPointsAwarded()).isEqualTo(0);

        ResponseEntity<EmployeeDashboardResponse> loserDashboardAfter = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loserToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(loserDashboardAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loserDashboardAfter.getBody()).isNotNull();
        assertThat(loserDashboardAfter.getBody().respectPoints()).isEqualTo(respectBefore);
        assertThat(loserDashboardAfter.getBody().karmaPoints()).isEqualTo(karmaBefore);

        ResponseEntity<String> duplicateAdminResolution = restTemplate.exchange(
                baseUrl("/api/admin/moderation/challenges/" + finishedMatch.challengeId() + "/resolve"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new AdminChallengeResolutionRequest(
                                ChallengeDisputeResolutionType.REJECT_NOT_FULFILLED,
                                "Second resolution should fail."
                        ),
                        authHeaders(adminToken)
                ),
                String.class
        );
        assertThat(duplicateAdminResolution.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private FinishedMatch createAndFinishWinningMatch(String winnerToken, String loserToken) {
        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Moderation Match " + UUID.randomUUID(), "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(winnerToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRoomResponse.getBody()).isNotNull();
        String roomId = createRoomResponse.getBody().id();

        ResponseEntity<LobbyRoomDetailResponse> joinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(loserToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ConnectFourGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                ConnectFourGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        playMove(roomId, winnerToken, 0);
        playMove(roomId, loserToken, 6);
        playMove(roomId, winnerToken, 1);
        playMove(roomId, loserToken, 6);
        playMove(roomId, winnerToken, 2);
        playMove(roomId, loserToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> finalMove = playMove(roomId, winnerToken, 3);

        assertThat(finalMove.getBody()).isNotNull();
        assertThat(finalMove.getBody().status()).isEqualTo("FINISHED");
        assertThat(finalMove.getBody().challenge()).isNotNull();
        return new FinishedMatch(finalMove.getBody().challenge().challengeId());
    }

    private ResponseEntity<ConnectFourGameStateResponse> playMove(String roomId, String token, int column) {
        ResponseEntity<ConnectFourGameStateResponse> response = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/move"),
                HttpMethod.POST,
                new HttpEntity<>(new ConnectFourMoveRequest(column), authHeaders(token)),
                ConnectFourGameStateResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Pw@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";
        String displayName = "PT15 " + label;

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, displayName, password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                AdminUserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();

        return new TestUser(createResponse.getBody().id(), email, password);
    }

    private void leaveMyRoomIfPresent(String token) {
        ResponseEntity<MyLobbyRoomResponse> myRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/my-room"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                MyLobbyRoomResponse.class
        );
        assertThat(myRoomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(myRoomResponse.getBody()).isNotNull();

        if (myRoomResponse.getBody().room() == null) {
            return;
        }

        ResponseEntity<LobbyRoomActionResponse> leaveResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + myRoomResponse.getBody().room().id() + "/leave"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(token)),
                LobbyRoomActionResponse.class
        );
        assertThat(leaveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String loginAndGetToken(String email, String password) {
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/login"),
                new LoginRequest(email, password),
                LoginResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private record TestUser(String userId, String email, String password) {
    }

    private record FinishedMatch(String challengeId) {
    }
}
