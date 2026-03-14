package com.officearcade.server.departments;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserListResponse;
import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.AssignUserDepartmentRequest;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.departments.dto.CreateDepartmentRequest;
import com.officearcade.server.departments.dto.DepartmentListResponse;
import com.officearcade.server.departments.dto.DepartmentResponse;
import com.officearcade.server.departments.dto.UpdateDepartmentRequest;
import com.officearcade.server.identity.AppRole;
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
class DepartmentManagementControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAllowAdminToManageDepartmentsAndAssignments() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        HttpHeaders adminHeaders = authHeaders(adminToken);

        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CreateDepartmentRequest createRequest = new CreateDepartmentRequest(
                "ENG_" + suffix,
                "Engineering " + suffix,
                "Product and platform engineering group."
        );

        ResponseEntity<DepartmentResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders),
                DepartmentResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().active()).isTrue();
        assertThat(createResponse.getBody().assignedUserCount()).isEqualTo(0);

        String departmentId = createResponse.getBody().id();

        ResponseEntity<DepartmentResponse> updateResponse = restTemplate.exchange(
                baseUrl("/api/admin/departments/" + departmentId),
                HttpMethod.PUT,
                new HttpEntity<>(new UpdateDepartmentRequest(
                        "ENG_" + suffix,
                        "Engineering Platform " + suffix,
                        "Updated department description."
                ), adminHeaders),
                DepartmentResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().displayName()).contains("Engineering Platform");

        TestUser managedUser = createEmployeeUser(adminToken, "dept-assignment");
        ResponseEntity<AdminUserResponse> assignResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + managedUser.id() + "/assign-department"),
                HttpMethod.POST,
                new HttpEntity<>(new AssignUserDepartmentRequest(departmentId), adminHeaders),
                AdminUserResponse.class
        );
        assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignResponse.getBody()).isNotNull();
        assertThat(assignResponse.getBody().department()).isNotNull();
        assertThat(assignResponse.getBody().department().id()).isEqualTo(departmentId);

        ResponseEntity<AdminUserListResponse> filteredUsers = restTemplate.exchange(
                baseUrl("/api/admin/users?departmentId=" + departmentId),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                AdminUserListResponse.class
        );
        assertThat(filteredUsers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filteredUsers.getBody()).isNotNull();
        assertThat(filteredUsers.getBody().users())
                .extracting(AdminUserResponse::id)
                .contains(managedUser.id());

        String userToken = loginAndGetToken(managedUser.email(), managedUser.password());

        ResponseEntity<ProfileMeResponse> profileResponse = restTemplate.exchange(
                baseUrl("/api/profile/me"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                ProfileMeResponse.class
        );
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).isNotNull();
        assertThat(profileResponse.getBody().department()).isNotNull();
        assertThat(profileResponse.getBody().department().id()).isEqualTo(departmentId);

        ResponseEntity<DashboardResponse> dashboardResponse = restTemplate.exchange(
                baseUrl("/api/employee/dashboard"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                DashboardResponse.class
        );
        assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboardResponse.getBody()).isNotNull();
        assertThat(dashboardResponse.getBody().department()).isNotNull();
        assertThat(dashboardResponse.getBody().department().id()).isEqualTo(departmentId);

        ResponseEntity<DepartmentResponse> deactivateResponse = restTemplate.exchange(
                baseUrl("/api/admin/departments/" + departmentId + "/deactivate"),
                HttpMethod.POST,
                new HttpEntity<>(adminHeaders),
                DepartmentResponse.class
        );
        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivateResponse.getBody()).isNotNull();
        assertThat(deactivateResponse.getBody().active()).isFalse();

        ResponseEntity<DepartmentListResponse> activeOnlyDirectory = restTemplate.exchange(
                baseUrl("/api/departments?activeOnly=true"),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                DepartmentListResponse.class
        );
        assertThat(activeOnlyDirectory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activeOnlyDirectory.getBody()).isNotNull();
        assertThat(activeOnlyDirectory.getBody().departments())
                .extracting(DepartmentResponse::id)
                .doesNotContain(departmentId);

        ResponseEntity<DepartmentListResponse> fullDirectory = restTemplate.exchange(
                baseUrl("/api/departments?activeOnly=false"),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                DepartmentListResponse.class
        );
        assertThat(fullDirectory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fullDirectory.getBody()).isNotNull();
        assertThat(fullDirectory.getBody().departments())
                .anySatisfy(department -> {
                    assertThat(department.id()).isEqualTo(departmentId);
                    assertThat(department.active()).isFalse();
                });

        ResponseEntity<AdminUserResponse> unassignResponse = restTemplate.exchange(
                baseUrl("/api/admin/users/" + managedUser.id() + "/assign-department"),
                HttpMethod.POST,
                new HttpEntity<>(new AssignUserDepartmentRequest(null), adminHeaders),
                AdminUserResponse.class
        );
        assertThat(unassignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unassignResponse.getBody()).isNotNull();
        assertThat(unassignResponse.getBody().department()).isNull();

        ResponseEntity<AdminUserListResponse> unassignedUsers = restTemplate.exchange(
                baseUrl("/api/admin/users?departmentId=UNASSIGNED"),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                AdminUserListResponse.class
        );
        assertThat(unassignedUsers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unassignedUsers.getBody()).isNotNull();
        assertThat(unassignedUsers.getBody().users())
                .extracting(AdminUserResponse::id)
                .contains(managedUser.id());
    }

    @Test
    void shouldRejectDuplicateDepartmentCodeAndDisplayName() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        HttpHeaders adminHeaders = authHeaders(adminToken);

        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String code = "OPS_" + suffix;
        String displayName = "Operations " + suffix;

        ResponseEntity<DepartmentResponse> firstCreate = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateDepartmentRequest(code, displayName, null), adminHeaders),
                DepartmentResponse.class
        );
        assertThat(firstCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> duplicateCode = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateDepartmentRequest(code.toLowerCase(), "Ops Other " + suffix, null), adminHeaders),
                String.class
        );
        assertThat(duplicateCode.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> duplicateName = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateDepartmentRequest("OPS_ALT_" + suffix, displayName.toLowerCase(), null), adminHeaders),
                String.class
        );
        assertThat(duplicateName.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldBlockEmployeeFromDepartmentAdminEndpoints() {
        String employeeToken = loginAndGetToken("employee@officearcade.local", "Employee@123");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/admin/departments"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(employeeToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Dept@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, "Department Test User", password, AppRole.EMPLOYEE, true),
                        authHeaders(adminToken)
                ),
                AdminUserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        return new TestUser(createResponse.getBody().id(), email, password);
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

    private record ProfileMeResponse(DepartmentSummary department) {
    }

    private record DashboardResponse(DepartmentSummary department) {
    }

    private record DepartmentSummary(String id, String code, String displayName, boolean active) {
    }
}
