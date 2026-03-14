package com.officearcade.server.employee.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.employee.dashboard.dto.EmployeeDashboardResponse;
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
class EmployeeDashboardControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnPersistedDashboardForEmployee() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");

        ResponseEntity<EmployeeDashboardResponse> response = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                EmployeeDashboardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("employee@officearcade.local");
        assertThat(response.getBody().role()).isEqualTo("EMPLOYEE");
        assertThat(response.getBody().level()).isGreaterThan(0);
        assertThat(response.getBody().gamesPlayed()).isGreaterThanOrEqualTo(response.getBody().wins() + response.getBody().losses());
        assertThat(response.getBody().enabledGameTypeCount()).isGreaterThan(0);
        assertThat(response.getBody().enabledGameTypes()).isNotEmpty();
        assertThat(response.getBody().winRatePercent()).isBetween(0.0, 100.0);
        assertThat(response.getBody().ownedCosmeticCount()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().equippedCosmeticCount()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().equippedCosmetics()).isNotNull();
    }

    @Test
    void shouldAllowAdminToReadDashboard() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");

        ResponseEntity<EmployeeDashboardResponse> response = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                EmployeeDashboardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRequireAuthenticationForDashboard() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl("/api/employee/dashboard"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
