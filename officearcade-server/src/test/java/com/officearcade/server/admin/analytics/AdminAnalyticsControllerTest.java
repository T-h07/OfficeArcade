package com.officearcade.server.admin.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.AssignUserDepartmentRequest;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.departments.dto.CreateDepartmentRequest;
import com.officearcade.server.departments.dto.DepartmentResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
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
class AdminAnalyticsControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnDashboardForAdmin() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        ResponseEntity<DashboardResponse> response = restTemplate.exchange(
                baseUrl("/api/admin/analytics/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                DashboardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().summary()).isNotNull();
        assertThat(response.getBody().activityTrend()).isNotNull();
        assertThat(response.getBody().generatedAt()).isNotBlank();
    }

    @Test
    void shouldBlockEmployeeFromAnalyticsEndpoint() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/admin/analytics/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldApplyDepartmentAndRangeFilters() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String departmentId = createDepartment(adminToken, "AN_" + suffix, "Analytics " + suffix);
        TestUser testUser = createEmployeeUser(adminToken, "analytics-filter");
        assignDepartment(adminToken, testUser.id(), departmentId);

        String userToken = loginAndGetToken(testUser.email(), testUser.password());
        ResponseEntity<String> roomCreateResponse = restTemplate.exchange(
                baseUrl("/api/lobby/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateLobbyRoomRequest("Analytics Room " + suffix, "CONNECT_FOUR", 2, 1, false, null),
                        authHeaders(userToken)
                ),
                String.class
        );
        assertThat(roomCreateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DashboardResponse> filteredResponse = restTemplate.exchange(
                baseUrl("/api/admin/analytics/dashboard?range=today&departmentId=" + departmentId),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                DashboardResponse.class
        );

        assertThat(filteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filteredResponse.getBody()).isNotNull();
        assertThat(filteredResponse.getBody().filters()).isNotNull();
        assertThat(filteredResponse.getBody().filters().selectedDepartment()).isNotNull();
        assertThat(filteredResponse.getBody().filters().selectedDepartment().id()).isEqualTo(departmentId);
        assertThat(filteredResponse.getBody().summary()).isNotNull();
        assertThat(filteredResponse.getBody().summary().totalEnabledUsers()).isGreaterThanOrEqualTo(1);
        assertThat(filteredResponse.getBody().summary().roomsCreatedInRange()).isGreaterThanOrEqualTo(1);
        assertThat(filteredResponse.getBody().departments()).isNotEmpty();
        assertThat(filteredResponse.getBody().departments())
                .allSatisfy(department -> assertThat(department.departmentId()).isEqualTo(departmentId));
    }

    private String createDepartment(String adminToken, String code, String displayName) {
        ResponseEntity<DepartmentResponse> response = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateDepartmentRequest(code, displayName, null), authHeaders(adminToken)),
                DepartmentResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Analytics@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, "Analytics Test User", password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                AdminUserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        return new TestUser(createResponse.getBody().id(), email, password);
    }

    private void assignDepartment(String adminToken, String userId, String departmentId) {
        ResponseEntity<AdminUserResponse> response = restTemplate.exchange(
                baseUrl("/api/admin/users/" + userId + "/assign-department"),
                HttpMethod.POST,
                new HttpEntity<>(new AssignUserDepartmentRequest(departmentId), authHeaders(adminToken)),
                AdminUserResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

    private record DashboardResponse(
            FiltersResponse filters,
            SummaryResponse summary,
            List<DepartmentResponseView> departments,
            List<ActivityPointResponse> activityTrend,
            String generatedAt
    ) {
    }

    private record FiltersResponse(
            String range,
            String departmentFilter,
            DepartmentSummary selectedDepartment
    ) {
    }

    private record DepartmentSummary(String id, String code, String displayName, boolean active) {
    }

    private record SummaryResponse(
            int totalEnabledUsers,
            int roomsCreatedInRange
    ) {
    }

    private record DepartmentResponseView(
            String departmentId,
            int userCount,
            int matchParticipationsInRange
    ) {
    }

    private record ActivityPointResponse(
            String date,
            int activeUsers,
            int matchesPlayed,
            int roomsCreated
    ) {
    }
}
