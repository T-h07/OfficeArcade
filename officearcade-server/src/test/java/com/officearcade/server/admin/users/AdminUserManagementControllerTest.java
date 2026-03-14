package com.officearcade.server.admin.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserListResponse;
import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.admin.users.dto.ResetUserPasswordRequest;
import com.officearcade.server.admin.users.dto.UpdateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.identity.AppRole;
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
class AdminUserManagementControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAllowAdminToManageUsersAndReflectAuthChanges() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        HttpHeaders adminHeaders = authHeaders(adminToken);

        ResponseEntity<AdminUserListResponse> initialListResponse = restTemplate.exchange(
                baseUrl("/api/admin/users?search=admin"),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                AdminUserListResponse.class
        );
        assertThat(initialListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initialListResponse.getBody()).isNotNull();
        assertThat(initialListResponse.getBody().users()).isNotEmpty();

        CreateAdminUserRequest createRequest = new CreateAdminUserRequest(
                "qa.employee@officearcade.local",
                "QA Employee",
                "Employee@456",
                AppRole.EMPLOYEE,
                true
        );

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders),
                AdminUserResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().email()).isEqualTo("qa.employee@officearcade.local");
        assertThat(createResponse.getBody().role()).isEqualTo("EMPLOYEE");

        String createdUserId = createResponse.getBody().id();

        ResponseEntity<AdminUserResponse> detailsResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + createdUserId),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                AdminUserResponse.class
        );
        assertThat(detailsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResponse.getBody()).isNotNull();
        assertThat(detailsResponse.getBody().displayName()).isEqualTo("QA Employee");

        UpdateAdminUserRequest updateRequest = new UpdateAdminUserRequest(
                "qa.employee@officearcade.local",
                "QA Employee Updated",
                AppRole.EMPLOYEE
        );
        ResponseEntity<AdminUserResponse> updateResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + createdUserId),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders),
                AdminUserResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().displayName()).isEqualTo("QA Employee Updated");

        ResponseEntity<String> resetPasswordResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + createdUserId + "/reset-password"),
                HttpMethod.POST,
                new HttpEntity<>(new ResetUserPasswordRequest("Updated@456"), adminHeaders),
                String.class
        );
        assertThat(resetPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<LoginResponse> newPasswordLoginResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/login"),
                new LoginRequest("qa.employee@officearcade.local", "Updated@456"),
                LoginResponse.class
        );
        assertThat(newPasswordLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newPasswordLoginResponse.getBody()).isNotNull();

        HttpHeaders employeeHeaders = authHeaders(newPasswordLoginResponse.getBody().accessToken());

        ResponseEntity<AdminUserResponse> deactivateResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + createdUserId + "/deactivate"),
                HttpMethod.POST,
                new HttpEntity<>(adminHeaders),
                AdminUserResponse.class
        );
        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivateResponse.getBody()).isNotNull();
        assertThat(deactivateResponse.getBody().enabled()).isFalse();

        ResponseEntity<String> meWhileInactiveResponse = restTemplate.exchange(
                baseUrl("/api/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(employeeHeaders),
                String.class
        );
        assertThat(meWhileInactiveResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<AdminUserResponse> activateResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + createdUserId + "/activate"),
                HttpMethod.POST,
                new HttpEntity<>(adminHeaders),
                AdminUserResponse.class
        );
        assertThat(activateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activateResponse.getBody()).isNotNull();
        assertThat(activateResponse.getBody().enabled()).isTrue();

        ResponseEntity<String> meWhileActiveResponse = restTemplate.exchange(
                baseUrl("/api/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(employeeHeaders),
                String.class
        );
        assertThat(meWhileActiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldBlockEmployeeFromAdminEndpoints() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldBlockDeactivationOfLastActiveAdmin() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/admin/users/user-admin-001/deactivate"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(adminToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
