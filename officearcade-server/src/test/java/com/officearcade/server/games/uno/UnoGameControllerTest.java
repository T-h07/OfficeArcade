package com.officearcade.server.games.uno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.games.uno.dto.UnoGameStateResponse;
import com.officearcade.server.games.uno.dto.UnoPlayCardRequest;
import com.officearcade.server.games.uno.persistence.UnoGameEntity;
import com.officearcade.server.games.uno.persistence.UnoGameEntityRepository;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class UnoGameControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UnoGameEntityRepository unoGameEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldValidateTurnOrderApplyActionCardsAndFinishMatch() {
        String hostToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String secondPlayerToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(hostToken);
        leaveMyRoomIfPresent(secondPlayerToken);

        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("PT19 UNO Room", "UNO", 2, 1, false, null),
                        authHeaders(hostToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRoomResponse.getBody()).isNotNull();
        String roomId = createRoomResponse.getBody().id();
        String hostUserId = createRoomResponse.getBody().hostUserId();

        ResponseEntity<LobbyRoomDetailResponse> joinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(secondPlayerToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinResponse.getBody()).isNotNull();
        assertThat(joinResponse.getBody().currentPlayers()).isEqualTo(2);
        String secondUserId = joinResponse.getBody().members().stream()
                .map(member -> member.userId())
                .filter(userId -> !userId.equals(hostUserId))
                .findFirst()
                .orElseThrow();

        ResponseEntity<UnoGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(hostToken)),
                UnoGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();
        assertThat(startResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(startResponse.getBody().players()).hasSize(2);

        ResponseEntity<String> outOfTurnResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/play"),
                HttpMethod.POST,
                new HttpEntity<>(new UnoPlayCardRequest("RED:0"), authHeaders(secondPlayerToken)),
                String.class
        );
        assertThat(outOfTurnResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        configureDeterministicState(
                roomId,
                hostUserId,
                secondUserId,
                List.of("RED:REVERSE", "BLUE:3"),
                List.of("GREEN:1"),
                List.of("BLUE:7", "GREEN:4", "YELLOW:1"),
                List.of("RED:5"),
                0,
                1,
                "RED",
                0
        );

        ResponseEntity<String> drawWithPlayableCard = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/draw"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(hostToken)),
                String.class
        );
        assertThat(drawWithPlayableCard.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<UnoGameStateResponse> reversePlayResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/play"),
                HttpMethod.POST,
                new HttpEntity<>(new UnoPlayCardRequest("RED:REVERSE"), authHeaders(hostToken)),
                UnoGameStateResponse.class
        );
        assertThat(reversePlayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reversePlayResponse.getBody()).isNotNull();
        assertThat(reversePlayResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(reversePlayResponse.getBody().currentTurnUserId()).isEqualTo(hostUserId);

        configureDeterministicState(
                roomId,
                hostUserId,
                secondUserId,
                List.of("RED:DRAW_TWO", "BLUE:8"),
                List.of("BLUE:2"),
                List.of("YELLOW:9", "GREEN:6", "BLUE:4", "RED:1"),
                List.of("RED:9"),
                0,
                1,
                "RED",
                5
        );

        ResponseEntity<UnoGameStateResponse> drawTwoPlayResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/play"),
                HttpMethod.POST,
                new HttpEntity<>(new UnoPlayCardRequest("RED:DRAW_TWO"), authHeaders(hostToken)),
                UnoGameStateResponse.class
        );
        assertThat(drawTwoPlayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(drawTwoPlayResponse.getBody()).isNotNull();
        assertThat(drawTwoPlayResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(drawTwoPlayResponse.getBody().currentTurnUserId()).isEqualTo(hostUserId);
        assertThat(drawTwoPlayResponse.getBody().players().stream()
                .filter(player -> player.userId().equals(secondUserId))
                .findFirst()
                .orElseThrow()
                .handCount()).isEqualTo(3);

        configureDeterministicState(
                roomId,
                hostUserId,
                secondUserId,
                List.of("RED:5"),
                List.of("BLUE:2"),
                List.of("YELLOW:8", "GREEN:2"),
                List.of("RED:1"),
                0,
                1,
                "RED",
                10
        );

        ResponseEntity<UnoGameStateResponse> winningPlayResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/play"),
                HttpMethod.POST,
                new HttpEntity<>(new UnoPlayCardRequest("RED:5"), authHeaders(hostToken)),
                UnoGameStateResponse.class
        );
        assertThat(winningPlayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(winningPlayResponse.getBody()).isNotNull();
        assertThat(winningPlayResponse.getBody().status()).isEqualTo("FINISHED");
        assertThat(winningPlayResponse.getBody().winnerUserId()).isEqualTo(hostUserId);

        ResponseEntity<String> postGamePlayResponse = restTemplate.exchange(
                baseUrl("/api/games/uno/room/" + roomId + "/draw"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(secondPlayerToken)),
                String.class
        );
        assertThat(postGamePlayResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(secondPlayerToken);
        leaveMyRoomIfPresent(hostToken);
    }

    private void configureDeterministicState(
            String roomIdText,
            String hostUserId,
            String secondUserId,
            List<String> hostHand,
            List<String> secondHand,
            List<String> drawPile,
            List<String> discardPile,
            int currentTurnIndex,
            int direction,
            String currentColor,
            int moveCount
    ) {
        UUID roomId = UUID.fromString(roomIdText);
        UnoGameEntity game = unoGameEntityRepository.findByRoom_Id(roomId).orElseThrow();

        Map<String, List<String>> hands = new LinkedHashMap<>();
        hands.put(hostUserId, hostHand);
        hands.put(secondUserId, secondHand);

        game.setStatus(UnoGameStatus.ACTIVE);
        game.setPlayerOrderState(writeJson(List.of(hostUserId, secondUserId)));
        game.setCurrentTurnIndex(currentTurnIndex);
        game.setDirection(direction);
        game.setCurrentColor(currentColor);
        game.setDrawPileState(writeJson(drawPile));
        game.setDiscardPileState(writeJson(discardPile));
        game.setHandsState(writeJson(hands));
        game.setWinnerUser(null);
        game.setMoveCount(moveCount);
        game.setPlayLimitsApplied(false);
        game.setStartedAt(Instant.now().minusSeconds(30));
        game.setEndedAt(null);
        unoGameEntityRepository.save(game);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize deterministic UNO state", ex);
        }
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
