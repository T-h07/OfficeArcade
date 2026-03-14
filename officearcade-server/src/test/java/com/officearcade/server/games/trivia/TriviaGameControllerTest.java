package com.officearcade.server.games.trivia;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.games.trivia.dto.TriviaAnswerRequest;
import com.officearcade.server.games.trivia.dto.TriviaGameStateResponse;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class TriviaGameControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRunTriviaRoundsValidateAnswersAndFinishWithStableScores() {
        String hostToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String secondPlayerToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(hostToken);
        leaveMyRoomIfPresent(secondPlayerToken);

        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("PT13 Trivia Battle", "TRIVIA", 2, 3, false, null),
                        authHeaders(hostToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRoomResponse.getBody()).isNotNull();
        String roomId = createRoomResponse.getBody().id();

        ResponseEntity<LobbyRoomDetailResponse> joinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(secondPlayerToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinResponse.getBody()).isNotNull();
        assertThat(joinResponse.getBody().currentPlayers()).isEqualTo(2);

        ResponseEntity<TriviaGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/trivia/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(hostToken)),
                TriviaGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();
        assertThat(startResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(startResponse.getBody().currentRound()).isEqualTo(1);
        assertThat(startResponse.getBody().currentQuestion()).isNotNull();

        ResponseEntity<String> secondStartResponse = restTemplate.exchange(
                baseUrl("/api/games/trivia/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(hostToken)),
                String.class
        );
        assertThat(secondStartResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Map<String, Integer> expectedScores = new HashMap<>();
        for (var player : startResponse.getBody().players()) {
            expectedScores.put(player.userId(), player.score());
        }

        TriviaGameStateResponse latestState = startResponse.getBody();
        int totalRounds = startResponse.getBody().totalRounds();

        for (int round = 1; round <= totalRounds; round++) {
            ResponseEntity<TriviaGameStateResponse> roundStateResponse = restTemplate.exchange(
                    baseUrl("/api/games/trivia/room/" + roomId),
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(hostToken)),
                    TriviaGameStateResponse.class
            );
            assertThat(roundStateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(roundStateResponse.getBody()).isNotNull();
            assertThat(roundStateResponse.getBody().status()).isEqualTo("ACTIVE");
            assertThat(roundStateResponse.getBody().currentRound()).isEqualTo(round);

            if (round == 1) {
                ResponseEntity<String> invalidOptionResponse = restTemplate.exchange(
                        baseUrl("/api/games/trivia/room/" + roomId + "/answer"),
                        HttpMethod.POST,
                        new HttpEntity<>(new TriviaAnswerRequest(9), authHeaders(hostToken)),
                        String.class
                );
                assertThat(invalidOptionResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<TriviaGameStateResponse> firstAnswerResponse = restTemplate.exchange(
                    baseUrl("/api/games/trivia/room/" + roomId + "/answer"),
                    HttpMethod.POST,
                    new HttpEntity<>(new TriviaAnswerRequest(round % 4), authHeaders(hostToken)),
                    TriviaGameStateResponse.class
            );
            assertThat(firstAnswerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstAnswerResponse.getBody()).isNotNull();
            assertThat(firstAnswerResponse.getBody().status()).isEqualTo("ACTIVE");

            ResponseEntity<String> duplicateAnswerResponse = restTemplate.exchange(
                    baseUrl("/api/games/trivia/room/" + roomId + "/answer"),
                    HttpMethod.POST,
                    new HttpEntity<>(new TriviaAnswerRequest((round + 1) % 4), authHeaders(hostToken)),
                    String.class
            );
            assertThat(duplicateAnswerResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            ResponseEntity<TriviaGameStateResponse> secondAnswerResponse = restTemplate.exchange(
                    baseUrl("/api/games/trivia/room/" + roomId + "/answer"),
                    HttpMethod.POST,
                    new HttpEntity<>(new TriviaAnswerRequest((round + 2) % 4), authHeaders(secondPlayerToken)),
                    TriviaGameStateResponse.class
            );
            assertThat(secondAnswerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondAnswerResponse.getBody()).isNotNull();
            assertThat(secondAnswerResponse.getBody().lastRoundOutcome()).isNotNull();
            assertThat(secondAnswerResponse.getBody().lastRoundOutcome().roundNumber()).isEqualTo(round);
            assertThat(secondAnswerResponse.getBody().lastRoundOutcome().playerAnswers()).hasSize(2);

            for (var playerAnswer : secondAnswerResponse.getBody().lastRoundOutcome().playerAnswers()) {
                if (playerAnswer.correct()) {
                    expectedScores.computeIfPresent(playerAnswer.userId(), (ignoredKey, score) -> score + 1);
                }
            }

            if (round < totalRounds) {
                assertThat(secondAnswerResponse.getBody().status()).isEqualTo("ACTIVE");
                assertThat(secondAnswerResponse.getBody().currentRound()).isEqualTo(round + 1);
            } else {
                assertThat(secondAnswerResponse.getBody().status()).isEqualTo("FINISHED");
            }

            latestState = secondAnswerResponse.getBody();
        }

        assertThat(latestState).isNotNull();
        assertThat(latestState.status()).isEqualTo("FINISHED");
        for (var player : latestState.players()) {
            assertThat(player.score()).isEqualTo(expectedScores.get(player.userId()));
        }

        ResponseEntity<String> postGameAnswerResponse = restTemplate.exchange(
                baseUrl("/api/games/trivia/room/" + roomId + "/answer"),
                HttpMethod.POST,
                new HttpEntity<>(new TriviaAnswerRequest(0), authHeaders(secondPlayerToken)),
                String.class
        );
        assertThat(postGameAnswerResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(secondPlayerToken);
        leaveMyRoomIfPresent(hostToken);
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
