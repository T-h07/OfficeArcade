package com.officearcade.server.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.officearcade.server.admin.users.dto.AdminUserResponse;
import com.officearcade.server.admin.users.dto.CreateAdminUserRequest;
import com.officearcade.server.auth.dto.LoginRequest;
import com.officearcade.server.auth.dto.LoginResponse;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.moderation.dto.AdminSuspendUserRequest;
import com.officearcade.server.moderation.dto.ModerationUserStateResponse;
import com.officearcade.server.notifications.dto.NotificationListResponse;
import com.officearcade.server.notifications.dto.NotificationReadAllResponse;
import com.officearcade.server.notifications.dto.NotificationResponse;
import com.officearcade.server.notifications.dto.NotificationUnreadCountResponse;
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
class NotificationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldPersistReadStateAndPreventCrossUserReadActions() {
        String adminToken = loginAndGetToken("admin@officearcade.local", "Admin@123");
        TestUser targetUser = createEmployeeUser(adminToken, "notify-target");
        TestUser otherUser = createEmployeeUser(adminToken, "notify-other");

        String targetToken = loginAndGetToken(targetUser.email(), targetUser.password());
        String otherToken = loginAndGetToken(otherUser.email(), otherUser.password());

        ResponseEntity<ModerationUserStateResponse> suspendResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/users/" + targetUser.userId() + "/suspend"),
                HttpMethod.POST,
                new HttpEntity<>(new AdminSuspendUserRequest("Temporary moderation status update."), authHeaders(adminToken)),
                ModerationUserStateResponse.class
        );
        assertThat(suspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ModerationUserStateResponse> unsuspendResponse = restTemplate.exchange(
                baseUrl("/api/admin/moderation/users/" + targetUser.userId() + "/unsuspend"),
                HttpMethod.POST,
                new HttpEntity<>(new AdminSuspendUserRequest("Restore access after review."), authHeaders(adminToken)),
                ModerationUserStateResponse.class
        );
        assertThat(unsuspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<NotificationListResponse> listResponse = listNotifications(targetToken, 25);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody().notifications()).isNotEmpty();
        assertThat(listResponse.getBody().unreadCount()).isGreaterThanOrEqualTo(2);
        assertThat(listResponse.getBody().notifications())
                .extracting(NotificationResponse::type)
                .contains("MODERATION_STATUS_UPDATE");

        NotificationResponse unreadNotification = listResponse.getBody().notifications().stream()
                .filter(NotificationResponse::unread)
                .findFirst()
                .orElseThrow();

        ResponseEntity<NotificationUnreadCountResponse> unreadBeforeResponse = restTemplate.exchange(
                baseUrl("/api/notifications/me/unread-count"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                NotificationUnreadCountResponse.class
        );
        assertThat(unreadBeforeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unreadBeforeResponse.getBody()).isNotNull();
        int unreadBefore = unreadBeforeResponse.getBody().unreadCount();
        assertThat(unreadBefore).isGreaterThanOrEqualTo(2);

        ResponseEntity<String> crossUserReadAttempt = restTemplate.exchange(
                baseUrl("/api/notifications/" + unreadNotification.id() + "/read"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(otherToken)),
                String.class
        );
        assertThat(crossUserReadAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<NotificationResponse> markOneReadResponse = restTemplate.exchange(
                baseUrl("/api/notifications/" + unreadNotification.id() + "/read"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(targetToken)),
                NotificationResponse.class
        );
        assertThat(markOneReadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(markOneReadResponse.getBody()).isNotNull();
        assertThat(markOneReadResponse.getBody().unread()).isFalse();
        assertThat(markOneReadResponse.getBody().readAt()).isNotNull();

        ResponseEntity<NotificationUnreadCountResponse> unreadAfterOneRead = restTemplate.exchange(
                baseUrl("/api/notifications/me/unread-count"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                NotificationUnreadCountResponse.class
        );
        assertThat(unreadAfterOneRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unreadAfterOneRead.getBody()).isNotNull();
        assertThat(unreadAfterOneRead.getBody().unreadCount()).isEqualTo(Math.max(unreadBefore - 1, 0));

        ResponseEntity<NotificationReadAllResponse> readAllResponse = restTemplate.exchange(
                baseUrl("/api/notifications/me/read-all"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(targetToken)),
                NotificationReadAllResponse.class
        );
        assertThat(readAllResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readAllResponse.getBody()).isNotNull();
        assertThat(readAllResponse.getBody().status()).isEqualTo("OK");
        assertThat(readAllResponse.getBody().markedCount()).isGreaterThanOrEqualTo(1);

        ResponseEntity<NotificationUnreadCountResponse> unreadAfterReadAll = restTemplate.exchange(
                baseUrl("/api/notifications/me/unread-count"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(targetToken)),
                NotificationUnreadCountResponse.class
        );
        assertThat(unreadAfterReadAll.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unreadAfterReadAll.getBody()).isNotNull();
        assertThat(unreadAfterReadAll.getBody().unreadCount()).isZero();

        ResponseEntity<NotificationListResponse> listAfterReadAll = listNotifications(targetToken, 25);
        assertThat(listAfterReadAll.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listAfterReadAll.getBody()).isNotNull();
        assertThat(listAfterReadAll.getBody().notifications())
                .allSatisfy(notification -> assertThat(notification.unread()).isFalse());
    }

    private ResponseEntity<NotificationListResponse> listNotifications(String token, int limit) {
        return restTemplate.exchange(
                baseUrl("/api/notifications/me?limit=" + limit),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                NotificationListResponse.class
        );
    }

    private TestUser createEmployeeUser(String adminToken, String label) {
        String unique = label + "+" + UUID.randomUUID();
        String email = unique + "@officearcade.local";
        String password = "Pw@" + Math.abs(UUID.randomUUID().hashCode()) + "aB";
        String displayName = "Notify " + label;

        ResponseEntity<AdminUserResponse> createResponse = restTemplate.exchange(
                baseUrl("/api/admin/users"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAdminUserRequest(email, displayName, password, AppRole.EMPLOYEE, true),
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private record TestUser(String userId, String email, String password) {
    }
}
