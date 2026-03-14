package com.officearcade.server.realtime;

import com.officearcade.server.security.JwtService;
import com.officearcade.server.security.JwtUserClaims;
import com.officearcade.server.security.OfficeArcadePrincipal;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class RealtimeJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserAccountService userAccountService;

    public RealtimeJwtChannelInterceptor(JwtService jwtService, UserAccountService userAccountService) {
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            UsernamePasswordAuthenticationToken authentication = authenticate(accessor);
            accessor.setUser(authentication);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && accessor.getUser() == null) {
            throw new AccessDeniedException("Realtime subscription requires authentication.");
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = resolveAuthorizationHeader(accessor);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Realtime connect request is missing a bearer token.");
        }

        String token = authorizationHeader.substring(7).trim();
        Optional<JwtUserClaims> claims = jwtService.parseAccessToken(token);
        if (claims.isEmpty()) {
            throw new AccessDeniedException("Realtime connect token is invalid.");
        }

        Optional<UserAccount> user = userAccountService.findById(claims.get().userId());
        if (user.isEmpty() || !user.get().enabled()) {
            throw new AccessDeniedException("Realtime connect user is unavailable.");
        }

        OfficeArcadePrincipal principal = new OfficeArcadePrincipal(
                user.get().id(),
                user.get().email(),
                user.get().displayName(),
                user.get().role(),
                user.get().enabled()
        );

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private static String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            return header;
        }
        return accessor.getFirstNativeHeader("authorization");
    }
}
