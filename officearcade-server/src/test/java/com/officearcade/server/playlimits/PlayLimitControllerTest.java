package com.officearcade.server.playlimits;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourGameStateResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourMoveRequest;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import com.officearcade.server.playlimits.dto.PlayLimitSummaryResponse;
import com.officearcade.server.playlimits.persistence.UserPlayLimitStateEntity;
import com.officearcade.server.playlimits.persistence.UserPlayLimitStateEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "officearcade.play-limits.daily-game-limit=5",
                "officearcade.play-limits.cooldown-minutes=90",
                "officearcade.play-limits.timezone=Europe/Berlin"
        }
)
class PlayLimitControllerTest {

    private static final ZoneId POLICY_ZONE_ID = ZoneId.of("Europe/Berlin");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private UserPlayLimitStateEntityRepository userPlayLimitStateEntityRepository;

    @AfterEach
    void cleanPlayLimitState() {
        userPlayLimitStateEntityRepository.deleteAll();
    }

    @Test
    void shouldReturnAuthenticatedPlayLimitSummary() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        leaveMyRoomIfPresent(employeeToken);

        ResponseEntity<PlayLimitSummaryResponse> response = restTemplate.exchange(
                baseUrl("/api/play-limits/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                PlayLimitSummaryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().dailyGameLimit()).isEqualTo(5);
        assertThat(response.getBody().gamesPlayedToday()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().gamesRemainingToday()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().eligibilityReason()).isIn("ELIGIBLE", "COOLDOWN_ACTIVE", "DAILY_LIMIT_REACHED");
    }

    @Test
    void shouldBlockRoomCreationWhenDailyLimitReached() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        leaveMyRoomIfPresent(employeeToken);

        upsertPlayLimitState("employee@officearcade.local", 5, null, Instant.now().minusSeconds(120));

        ResponseEntity<String> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Limit Block Room", "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(employeeToken)
                ),
                String.class
        );

        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldApplyCooldownAfterCompletedConnectFourMatch() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(employeeToken);
        leaveMyRoomIfPresent(adminToken);

        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Cooldown Connect Four", "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(employeeToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRoomResponse.getBody()).isNotNull();
        String roomId = createRoomResponse.getBody().id();

        ResponseEntity<LobbyRoomDetailResponse> joinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(adminToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ConnectFourGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(employeeToken)),
                ConnectFourGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        playMove(roomId, employeeToken, 0);
        playMove(roomId, adminToken, 6);
        playMove(roomId, employeeToken, 1);
        playMove(roomId, adminToken, 6);
        playMove(roomId, employeeToken, 2);
        playMove(roomId, adminToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> finalMove = playMove(roomId, employeeToken, 3);

        assertThat(finalMove.getBody()).isNotNull();
        assertThat(finalMove.getBody().status()).isEqualTo("FINISHED");

        ResponseEntity<PlayLimitSummaryResponse> employeeLimitsResponse = restTemplate.exchange(
                baseUrl("/api/play-limits/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                PlayLimitSummaryResponse.class
        );
        assertThat(employeeLimitsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(employeeLimitsResponse.getBody()).isNotNull();
        assertThat(employeeLimitsResponse.getBody().gamesPlayedToday()).isGreaterThanOrEqualTo(1);
        assertThat(employeeLimitsResponse.getBody().cooldownActive()).isTrue();
        assertThat(employeeLimitsResponse.getBody().canPlayNow()).isFalse();
        assertThat(employeeLimitsResponse.getBody().eligibilityReason()).isEqualTo("COOLDOWN_ACTIVE");

        ResponseEntity<String> blockedCreateRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Blocked by Cooldown", "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(employeeToken)
                ),
                String.class
        );
        assertThat(blockedCreateRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(adminToken);
        leaveMyRoomIfPresent(employeeToken);
    }

    private ResponseEntity<ConnectFourGameStateResponse> playMove(String roomId, String token, int column) {
        ResponseEntity<ConnectFourGameStateResponse> response = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/move"),
                HttpMethod.POST,
                new HttpEntity<>(new ConnectFourMoveRequest(column), authHeaders(token)),
                ConnectFourGameStateResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response;
    }

    private void upsertPlayLimitState(String email, int gamesPlayedToday, Instant cooldownUntil, Instant lastCompletedAt) {
        UserEntity user = userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Missing seeded user: " + email));

        UserPlayLimitStateEntity state = userPlayLimitStateEntityRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPlayLimitStateEntity created = new UserPlayLimitStateEntity();
                    created.setUserId(user.getId());
                    return created;
                });

        state.setGamesPlayedDate(LocalDate.now(POLICY_ZONE_ID));
        state.setGamesPlayedToday(gamesPlayedToday);
        state.setCooldownUntil(cooldownUntil);
        state.setLastCompletedGameAt(lastCompletedAt);
        userPlayLimitStateEntityRepository.save(state);
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
}
