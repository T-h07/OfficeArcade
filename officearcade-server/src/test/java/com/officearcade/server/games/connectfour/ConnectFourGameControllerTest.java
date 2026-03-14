package com.officearcade.server.games.connectfour;

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
import java.util.List;
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
class ConnectFourGameControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldStartMatchEnforceTurnOrderAndDetectWin() {
        String hostToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String secondPlayerToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(hostToken);
        leaveMyRoomIfPresent(secondPlayerToken);

        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("PT08 Connect Four", "CONNECT_FOUR", 2, 1, false, null),
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

        ResponseEntity<ConnectFourGameStateResponse> startResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/start"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(hostToken)),
                ConnectFourGameStateResponse.class
        );
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(startResponse.getBody()).isNotNull();
        assertThat(startResponse.getBody().status()).isEqualTo("ACTIVE");
        assertThat(startResponse.getBody().moveCount()).isEqualTo(0);

        ResponseEntity<String> wrongTurnMoveResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/move"),
                HttpMethod.POST,
                new HttpEntity<>(new ConnectFourMoveRequest(3), authHeaders(secondPlayerToken)),
                String.class
        );
        assertThat(wrongTurnMoveResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<ConnectFourGameStateResponse> move1 = playMove(roomId, hostToken, 0);
        ResponseEntity<ConnectFourGameStateResponse> move2 = playMove(roomId, secondPlayerToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> move3 = playMove(roomId, hostToken, 1);
        ResponseEntity<ConnectFourGameStateResponse> move4 = playMove(roomId, secondPlayerToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> move5 = playMove(roomId, hostToken, 2);
        ResponseEntity<ConnectFourGameStateResponse> move6 = playMove(roomId, secondPlayerToken, 6);
        ResponseEntity<ConnectFourGameStateResponse> finalMove = playMove(roomId, hostToken, 3);

        assertThat(move1.getBody()).isNotNull();
        assertThat(move2.getBody()).isNotNull();
        assertThat(move3.getBody()).isNotNull();
        assertThat(move4.getBody()).isNotNull();
        assertThat(move5.getBody()).isNotNull();
        assertThat(move6.getBody()).isNotNull();
        assertThat(finalMove.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalMove.getBody()).isNotNull();
        assertThat(finalMove.getBody().status()).isEqualTo("FINISHED");
        assertThat(finalMove.getBody().draw()).isFalse();
        assertThat(finalMove.getBody().winnerUserId()).isEqualTo(startResponse.getBody().playerOneUserId());
        assertThat(finalMove.getBody().moveCount()).isEqualTo(7);
        assertThat(finalMove.getBody().challenge()).isNotNull();
        assertThat(finalMove.getBody().challenge().status()).isEqualTo("PENDING");

        ResponseEntity<String> postGameMoveResponse = restTemplate.exchange(
                baseUrl("/api/games/connect-four/room/" + roomId + "/move"),
                HttpMethod.POST,
                new HttpEntity<>(new ConnectFourMoveRequest(4), authHeaders(secondPlayerToken)),
                String.class
        );
        assertThat(postGameMoveResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(secondPlayerToken);
        leaveMyRoomIfPresent(hostToken);
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
