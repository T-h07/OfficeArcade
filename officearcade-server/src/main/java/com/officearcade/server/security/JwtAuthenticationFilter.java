package com.officearcade.server.security;

import com.officearcade.server.identity.DevIdentityService;
import com.officearcade.server.identity.IdentityUser;
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
    private final DevIdentityService devIdentityService;

    public JwtAuthenticationFilter(JwtService jwtService, DevIdentityService devIdentityService) {
        this.jwtService = jwtService;
        this.devIdentityService = devIdentityService;
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
        Optional<IdentityUser> maybeUser = devIdentityService.findById(claims.userId());
        if (maybeUser.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        IdentityUser user = maybeUser.get();
        if (!user.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        OfficeArcadePrincipal principal = new OfficeArcadePrincipal(
                user.id(),
                user.email(),
                user.displayName(),
                user.role(),
                user.enabled()
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}
