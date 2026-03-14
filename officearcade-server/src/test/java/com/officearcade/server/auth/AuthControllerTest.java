package com.officearcade.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.auth.dto.CurrentUserResponse;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldLoginAndReadAuthenticatedUser() {
        LoginRequest request = new LoginRequest("admin@officearcade.local", "Admin@123");
        String loginUrl = "http://localhost:" + port + "/api/auth/login";

        ResponseEntity<LoginResponse> loginResponse =
                restTemplate.postForEntity(loginUrl, request, LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().accessToken()).isNotBlank();
        assertThat(loginResponse.getBody().user().role()).isEqualTo("ADMIN");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().accessToken());

        String meUrl = "http://localhost:" + port + "/api/auth/me";
        ResponseEntity<CurrentUserResponse> meResponse =
                restTemplate.exchange(meUrl, HttpMethod.GET, new HttpEntity<>(headers), CurrentUserResponse.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().user().email()).isEqualTo("admin@officearcade.local");
        assertThat(meResponse.getBody().user().role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectMeRouteWithoutToken() {
        String meUrl = "http://localhost:" + port + "/api/auth/me";

        ResponseEntity<String> response = restTemplate.getForEntity(meUrl, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldBlockEmployeeFromAdminOnlyRoute() {
        LoginRequest request = new LoginRequest("employee@officearcade.local", "Employee@123");
        String loginUrl = "http://localhost:" + port + "/api/auth/login";

        ResponseEntity<LoginResponse> loginResponse =
                restTemplate.postForEntity(loginUrl, request, LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().accessToken());

        String adminCheckUrl = "http://localhost:" + port + "/api/auth/role-check/admin";
        ResponseEntity<String> adminCheckResponse =
                restTemplate.exchange(adminCheckUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(adminCheckResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
