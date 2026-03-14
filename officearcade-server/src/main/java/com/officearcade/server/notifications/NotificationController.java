package com.officearcade.server.notifications;

import com.officearcade.server.notifications.dto.NotificationListResponse;
import com.officearcade.server.notifications.dto.NotificationReadAllResponse;
import com.officearcade.server.notifications.dto.NotificationResponse;
import com.officearcade.server.notifications.dto.NotificationUnreadCountResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public NotificationListResponse listMyNotifications(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @RequestParam(required = false) Integer limit
    ) {
        return notificationService.listNotificationsForUser(getRequiredPrincipal(principal).id(), limit);
    }

    @GetMapping("/me/unread-count")
    public NotificationUnreadCountResponse getMyUnreadCount(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return notificationService.getUnreadCountForUser(getRequiredPrincipal(principal).id());
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.OK)
    public NotificationResponse markNotificationAsRead(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String notificationId
    ) {
        return notificationService.markNotificationAsRead(getRequiredPrincipal(principal).id(), notificationId);
    }

    @PostMapping("/me/read-all")
    @ResponseStatus(HttpStatus.OK)
    public NotificationReadAllResponse markAllNotificationsAsRead(
            @AuthenticationPrincipal OfficeArcadePrincipal principal
    ) {
        return notificationService.markAllNotificationsAsRead(getRequiredPrincipal(principal).id());
    }

    private static OfficeArcadePrincipal getRequiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
