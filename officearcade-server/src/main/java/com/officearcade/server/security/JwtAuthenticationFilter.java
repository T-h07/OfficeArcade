package com.officearcade.server.security;

import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountService userAccountService;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountService userAccountService) {
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();
        Optional<JwtUserClaims> parsedClaims = jwtService.parseAccessToken(token);
        if (parsedClaims.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtUserClaims claims = parsedClaims.get();
        Optional<UserAccount> maybeUser = userAccountService.findById(claims.userId());
        if (maybeUser.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        UserAccount user = maybeUser.get();
        if (!user.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (user.suspended() && !isSuspendedAccessAllowed(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is currently suspended.");
            return;
        }

        OfficeArcadePrincipal principal = new OfficeArcadePrincipal(
                user.id(),
                user.email(),
                user.displayName(),
                user.role(),
                user.enabled(),
                user.suspended(),
                user.suspensionNote()
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }

    private static boolean isSuspendedAccessAllowed(HttpServletRequest request) {
        return "/api/auth/me".equals(request.getRequestURI());
    }
}
