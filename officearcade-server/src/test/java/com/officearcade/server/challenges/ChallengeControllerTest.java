package com.officearcade.server.challenges;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.challenges.dto.ChallengeListResponse;
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
class ChallengeControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateChallengeFromCompletedMatchAndApplyRespectOnConfirm() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        TestUser winner = createEmployeeUser(adminToken, "winner");
        TestUser loser = createEmployeeUser(adminToken, "loser");

        String winnerToken = loginAndGetToken(winner.email(), winner.password());
        String loserToken = loginAndGetToken(loser.email(), loser.password());

        leaveMyRoomIfPresent(winnerToken);
        leaveMyRoomIfPresent(loserToken);

        FinishedMatch finishedMatch = createAndFinishWinningMatch(winnerToken, loserToken);
        assertThat(finishedMatch.challengeId()).isNotBlank();

        ResponseEntity<ChallengeListResponse> winnerChallengesResponse = restTemplate.exchange(
                baseUrl("/api/challenges/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(winnerToken)),
                ChallengeListResponse.class
        );
        assertThat(winnerChallengesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(winnerChallengesResponse.getBody()).isNotNull();
        ChallengeSummaryResponse pendingForWinner = winnerChallengesResponse.getBody().challenges().stream()
                .filter(challenge -> challenge.id().equals(finishedMatch.challengeId()))
                .findFirst()
                .orElseThrow();
        assertThat(pendingForWinner.status()).isEqualTo("PENDING");
        assertThat(pendingForWinner.canResolve()).isTrue();
        assertThat(pendingForWinner.myRole()).isEqualTo("BENEFICIARY");

        ResponseEntity<ChallengeListResponse> loserChallengesResponse = restTemplate.exchange(
                baseUrl("/api/challenges/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loserToken)),
                ChallengeListResponse.class
        );
        assertThat(loserChallengesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loserChallengesResponse.getBody()).isNotNull();
        ChallengeSummaryResponse pendingForLoser = loserChallengesResponse.getBody().challenges().stream()
                .filter(challenge -> challenge.id().equals(finishedMatch.challengeId()))
                .findFirst()
                .orElseThrow();
        assertThat(pendingForLoser.canResolve()).isFalse();
        assertThat(pendingForLoser.myRole()).isEqualTo("OBLIGATED");

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

        ResponseEntity<String> unauthorizedResolution = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/confirm"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(loserToken)),
                String.class
        );
        assertThat(unauthorizedResolution.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ChallengeSummaryResponse> confirmResponse = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/confirm"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                ChallengeSummaryResponse.class
        );
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmResponse.getBody()).isNotNull();
        assertThat(confirmResponse.getBody().status()).isEqualTo("COMPLETED_CONFIRMED");
        assertThat(confirmResponse.getBody().respectPointsAwarded()).isEqualTo(10);
        assertThat(confirmResponse.getBody().karmaPointsAwarded()).isEqualTo(0);

        ResponseEntity<EmployeeDashboardResponse> loserDashboardAfter = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loserToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(loserDashboardAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loserDashboardAfter.getBody()).isNotNull();
        assertThat(loserDashboardAfter.getBody().respectPoints()).isEqualTo(respectBefore + 10);
        assertThat(loserDashboardAfter.getBody().karmaPoints()).isEqualTo(karmaBefore);

        ResponseEntity<String> duplicateConfirm = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/confirm"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                String.class
        );
        assertThat(duplicateConfirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldApplyKarmaOnReject() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        TestUser winner = createEmployeeUser(adminToken, "reject-winner");
        TestUser loser = createEmployeeUser(adminToken, "reject-loser");

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

        ResponseEntity<ChallengeSummaryResponse> rejectResponse = restTemplate.exchange(
                baseUrl("/api/challenges/" + finishedMatch.challengeId() + "/reject"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                ChallengeSummaryResponse.class
        );
        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejectResponse.getBody()).isNotNull();
        assertThat(rejectResponse.getBody().status()).isEqualTo("REJECTED");
        assertThat(rejectResponse.getBody().respectPointsAwarded()).isEqualTo(0);
        assertThat(rejectResponse.getBody().karmaPointsAwarded()).isEqualTo(5);

        ResponseEntity<EmployeeDashboardResponse> loserDashboardAfter = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loserToken)),
                EmployeeDashboardResponse.class
        );
        assertThat(loserDashboardAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loserDashboardAfter.getBody()).isNotNull();
        assertThat(loserDashboardAfter.getBody().respectPoints()).isEqualTo(respectBefore);
        assertThat(loserDashboardAfter.getBody().karmaPoints()).isEqualTo(karmaBefore + 5);
    }

    private FinishedMatch createAndFinishWinningMatch(String winnerToken, String loserToken) {
        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("RK Match " + UUID.randomUUID(), "CONNECT_FOUR", 2, 1, false, null),
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
        assertThat(joinResponse.getBody()).isNotNull();

        ResponseEntity<ConnectFourGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(winnerToken)),
                ConnectFourGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();

        playMove(roomId, winnerToken, 0);
        playMove(roomId, loserToken, 6);
        playMove(roomId, winnerToken, 1);
        playMove(roomId, loserToken, 6);
        playMove(roomId, winnerToken, 2);
        playMove(roomId, loserToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> finalMove = playMove(roomId, winnerToken, 3);

        assertThat(finalMove.getBody()).isNotNull();
        assertThat(finalMove.getBody().status()).isEqualTo("FINISHED");
        assertThat(finalMove.getBody().draw()).isFalse();
        assertThat(finalMove.getBody().challenge()).isNotNull();

        return new FinishedMatch(roomId, finalMove.getBody().challenge().challengeId());
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
        String displayName = "RK " + label;

        ResponseEntity<String> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, displayName, password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                String.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new TestUser(email, password);
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
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private record TestUser(String email, String password) {
    }

    private record FinishedMatch(String roomId, String challengeId) {
    }
}
