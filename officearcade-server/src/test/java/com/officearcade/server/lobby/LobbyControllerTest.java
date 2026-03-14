package com.officearcade.server.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.LobbyRoomListResponse;
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
class LobbyControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateJoinLeaveAndCloseRoomLifecycle() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(employeeToken);
        leaveMyRoomIfPresent(adminToken);

        ResponseEntity<LobbyRoomDetailResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Lunch UNO Room", "UNO", 2, 3, false, null),
                        authHeaders(employeeToken)
                ),
                LobbyRoomDetailResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().roomName()).isEqualTo("Lunch UNO Room");
        assertThat(createResponse.getBody().status()).isEqualTo("OPEN");
        assertThat(createResponse.getBody().currentPlayers()).isEqualTo(1);

        String roomId = createResponse.getBody().id();

        ResponseEntity<LobbyRoomListResponse> listResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                LobbyRoomListResponse.class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody().rooms().stream().map(room -> room.id())).contains(roomId);

        ResponseEntity<LobbyRoomDetailResponse> joinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(adminToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinResponse.getBody()).isNotNull();
        assertThat(joinResponse.getBody().status()).isEqualTo("FULL");
        assertThat(joinResponse.getBody().currentPlayers()).isEqualTo(2);

        ResponseEntity<LobbyRoomActionResponse> memberLeaveResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/leave"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(adminToken)),
                LobbyRoomActionResponse.class
        );
        assertThat(memberLeaveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberLeaveResponse.getBody()).isNotNull();
        assertThat(memberLeaveResponse.getBody().room()).isNotNull();
        assertThat(memberLeaveResponse.getBody().room().status()).isEqualTo("OPEN");
        assertThat(memberLeaveResponse.getBody().room().currentPlayers()).isEqualTo(1);

        ResponseEntity<LobbyRoomActionResponse> hostLeaveResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/leave"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(employeeToken)),
                LobbyRoomActionResponse.class
        );
        assertThat(hostLeaveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hostLeaveResponse.getBody()).isNotNull();
        assertThat(hostLeaveResponse.getBody().room()).isNotNull();
        assertThat(hostLeaveResponse.getBody().room().status()).isEqualTo("CLOSED");
        assertThat(hostLeaveResponse.getBody().room().members()).isEmpty();

        ResponseEntity<LobbyRoomListResponse> postCloseListResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                LobbyRoomListResponse.class
        );
        assertThat(postCloseListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postCloseListResponse.getBody()).isNotNull();
        assertThat(postCloseListResponse.getBody().rooms().stream().map(room -> room.id()))
                .doesNotContain(roomId);
    }

    @Test
    void shouldEnforcePrivatePasswordAndSingleRoomMembershipRule() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        leaveMyRoomIfPresent(employeeToken);
        leaveMyRoomIfPresent(adminToken);

        ResponseEntity<LobbyRoomDetailResponse> createPrivateRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Private Trivia", "TRIVIA", 3, 2, true, "Room@123"),
                        authHeaders(employeeToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createPrivateRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createPrivateRoomResponse.getBody()).isNotNull();
        String privateRoomId = createPrivateRoomResponse.getBody().id();

        ResponseEntity<String> wrongPasswordJoinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + privateRoomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest("wrong-pass"), authHeaders(adminToken)),
                String.class
        );
        assertThat(wrongPasswordJoinResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<LobbyRoomDetailResponse> correctPasswordJoinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + privateRoomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest("Room@123"), authHeaders(adminToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(correctPasswordJoinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(correctPasswordJoinResponse.getBody()).isNotNull();
        assertThat(correctPasswordJoinResponse.getBody().currentPlayers()).isEqualTo(2);

        ResponseEntity<String> secondRoomCreateWhileJoinedResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Second Room Attempt", "UNO", 4, 2, false, null),
                        authHeaders(adminToken)
                ),
                String.class
        );
        assertThat(secondRoomCreateWhileJoinedResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(adminToken);
        leaveMyRoomIfPresent(employeeToken);
    }

    @Test
    void shouldRejectJoinWhenRoomIsFull() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");

        leaveMyRoomIfPresent(adminToken);
        leaveMyRoomIfPresent(employeeToken);

        String thirdUserEmail = "room.tester+" + UUID.randomUUID() + "@officearcade.local";
        String thirdUserPassword = "RoomUser@123";

        ResponseEntity<String> createThirdUserResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(
                                thirdUserEmail,
                                "Room Tester",
                                thirdUserPassword,
                                AppRole.EMPLOYEE,
                                true
                        ),
                        authHeaders(adminToken)
                ),
                String.class
        );
        assertThat(createThirdUserResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<LobbyRoomDetailResponse> createRoomResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Compact Connect Room", "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(employeeToken)
                ),
                LobbyRoomDetailResponse.class
        );
        assertThat(createRoomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRoomResponse.getBody()).isNotNull();
        String roomId = createRoomResponse.getBody().id();

        ResponseEntity<LobbyRoomDetailResponse> adminJoinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(adminToken)),
                LobbyRoomDetailResponse.class
        );
        assertThat(adminJoinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminJoinResponse.getBody()).isNotNull();
        assertThat(adminJoinResponse.getBody().status()).isEqualTo("FULL");

        String thirdUserToken = loginAndGetToken(thirdUserEmail, thirdUserPassword);
        ResponseEntity<String> fullRoomJoinResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms/" + roomId + "/join"),
                HttpMethod.POST,
                new HttpEntity<>(new JoinLobbyRoomRequest(null), authHeaders(thirdUserToken)),
                String.class
        );
        assertThat(fullRoomJoinResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        leaveMyRoomIfPresent(adminToken);
        leaveMyRoomIfPresent(employeeToken);
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
