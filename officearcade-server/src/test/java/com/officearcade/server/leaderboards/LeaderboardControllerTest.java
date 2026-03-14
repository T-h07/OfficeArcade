package com.officearcade.server.leaderboards;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.AssignUserDepartmentRequest;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.departments.dto.CreateDepartmentRequest;
import com.officearcade.server.departments.dto.DepartmentResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
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
class LeaderboardControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerProfileEntityRepository playerProfileEntityRepository;

    @Test
    void shouldReturnWinsLeaderboardWithCurrentUserContext() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser userA = createEmployeeUser(adminToken, "lb-wins-a");
        TestUser userB = createEmployeeUser(adminToken, "lb-wins-b");

        setProfileStats(userA.id(), 5, 620, 25, 20, 5, 140, 4);
        setProfileStats(userB.id(), 4, 460, 16, 12, 4, 90, 2);

        String userAToken = loginAndGetToken(userA.email(), userA.password());

        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl("/api/leaderboards?type=WINS&limit=10"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAToken)),
                LeaderboardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        LeaderboardResponse leaderboard = response.getBody();

        assertThat(leaderboard.type()).isEqualTo("WINS");
        assertThat(leaderboard.entries()).isNotEmpty();
        assertThat(leaderboard.currentUserEligible()).isTrue();
        assertThat(leaderboard.currentUserEntry()).isNotNull();
        assertThat(leaderboard.currentUserEntry().userId()).isEqualTo(userA.id());

        if (leaderboard.entries().size() > 1) {
            assertThat(leaderboard.entries().get(0).wins())
                    .isGreaterThanOrEqualTo(leaderboard.entries().get(1).wins());
        }
    }

    @Test
    void shouldApplyWinRateMinimumMatchThreshold() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser lowActivityUser = createEmployeeUser(adminToken, "lb-winrate-low");
        TestUser strongUser = createEmployeeUser(adminToken, "lb-winrate-strong");

        setProfileStats(lowActivityUser.id(), 2, 210, 1, 1, 0, 40, 3);
        setProfileStats(strongUser.id(), 6, 840, 18, 14, 4, 180, 1);

        String lowActivityToken = loginAndGetToken(lowActivityUser.email(), lowActivityUser.password());

        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl("/api/leaderboards?type=WIN_RATE&limit=10"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(lowActivityToken)),
                LeaderboardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        LeaderboardResponse leaderboard = response.getBody();

        assertThat(leaderboard.type()).isEqualTo("WIN_RATE");
        assertThat(leaderboard.minimumCompletedMatches()).isEqualTo(5);
        assertThat(leaderboard.currentUserEligible()).isFalse();
        assertThat(leaderboard.currentUserEntry()).isNull();
        assertThat(leaderboard.currentUserNote()).contains("minimum completed matches");

        assertThat(leaderboard.entries())
                .allMatch(entry -> entry.wins() + entry.losses() >= leaderboard.minimumCompletedMatches());
    }

    @Test
    void shouldFilterLeaderboardByDepartmentAndUnassignedScope() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        String departmentId = createDepartment(adminToken, "LB_DEPT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        TestUser inDepartment = createEmployeeUser(adminToken, "lb-department-member");
        TestUser unassigned = createEmployeeUser(adminToken, "lb-unassigned-member");

        setProfileStats(inDepartment.id(), 4, 420, 14, 11, 3, 120, 2);
        setProfileStats(unassigned.id(), 4, 390, 13, 10, 3, 95, 3);
        assignDepartment(adminToken, inDepartment.id(), departmentId);

        String departmentUserToken = loginAndGetToken(inDepartment.email(), inDepartment.password());

        ResponseEntity<LeaderboardResponse> departmentFilteredResponse = restTemplate.exchange(
                baseUrl("/api/leaderboards?type=WINS&limit=10&departmentId=" + departmentId),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(departmentUserToken)),
                LeaderboardResponse.class
        );
        assertThat(departmentFilteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(departmentFilteredResponse.getBody()).isNotNull();
        assertThat(departmentFilteredResponse.getBody().entries()).isNotEmpty();
        assertThat(departmentFilteredResponse.getBody().entries())
                .allSatisfy(entry -> {
                    assertThat(entry.department()).isNotNull();
                    assertThat(entry.department().id()).isEqualTo(departmentId);
                });

        ResponseEntity<LeaderboardResponse> unassignedFilteredResponse = restTemplate.exchange(
                baseUrl("/api/leaderboards?type=WINS&limit=10&departmentId=UNASSIGNED"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(departmentUserToken)),
                LeaderboardResponse.class
        );
        assertThat(unassignedFilteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unassignedFilteredResponse.getBody()).isNotNull();
        assertThat(unassignedFilteredResponse.getBody().entries())
                .allMatch(entry -> entry.department() == null);
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Leaderboard@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, "Leaderboard Test User", password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                AdminUserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        return new TestUser(createResponse.getBody().id(), email, password);
    }

    private void setProfileStats(
            String userIdText,
            int level,
            int xp,
            int gamesPlayed,
            int wins,
            int losses,
            int respect,
            int karma
    ) {
        UUID userId = UUID.fromString(userIdText);
        PlayerProfileEntity profile = playerProfileEntityRepository.findByUserId(userId).orElseThrow();
        profile.setLevel(Math.max(level, 1));
        profile.setXp(Math.max(xp, 0));
        profile.setGamesPlayed(Math.max(gamesPlayed, 0));
        profile.setWins(Math.max(wins, 0));
        profile.setLosses(Math.max(losses, 0));
        profile.setRespectPoints(Math.max(respect, 0));
        profile.setKarmaPoints(Math.max(karma, 0));
        playerProfileEntityRepository.save(profile);
    }

    private String createDepartment(String adminToken, String code) {
        ResponseEntity<DepartmentResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateDepartmentRequest(code, "Leaderboard Department " + code, null),
                        authHeaders(adminToken)
                ),
                DepartmentResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        return createResponse.getBody().id();
    }

    private void assignDepartment(String adminToken, String userId, String departmentId) {
        ResponseEntity<AdminUserResponse> response = restTemplate.exchange(
                baseUrl("/api/admin/users/" + userId + "/assign-department"),
                HttpMethod.POST,
                new HttpEntity<>(new AssignUserDepartmentRequest(departmentId), authHeaders(adminToken)),
                AdminUserResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().department()).isNotNull();
        assertThat(response.getBody().department().id()).isEqualTo(departmentId);
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

    private record TestUser(String id, String email, String password) {
    }

    private record LeaderboardResponse(
            String type,
            int minimumCompletedMatches,
            List<LeaderboardEntry> entries,
            LeaderboardEntry currentUserEntry,
            boolean currentUserEligible,
            String currentUserNote
    ) {
    }

    private record LeaderboardEntry(
            String userId,
            int wins,
            int losses,
            boolean currentUser,
            DepartmentSummary department
    ) {
    }

    private record DepartmentSummary(
            String id,
            String code,
            String displayName,
            boolean active
    ) {
    }
}
