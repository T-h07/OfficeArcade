package com.officearcade.server.security;

import com.officearcade.server.identity.AppRole;
import com.officearcade.server.identity.IdentityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(IdentityUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.id())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("email", user.email())
                .claim("displayName", user.displayName())
                .claim("role", user.role().name())
                .signWith(signingKey)
                .compact();
    }

    public Optional<JwtUserClaims> parseAccessToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtProperties.issuer())
                    .build()
                    .parseSignedClaims(token);

            Claims claims = claimsJws.getPayload();
            AppRole role = AppRole.valueOf(claims.get("role", String.class));

            return Optional.of(new JwtUserClaims(
                    claims.getSubject(),
                    claims.get("email", String.class),
                    claims.get("displayName", String.class),
                    role
            ));
        } catch (IllegalArgumentException | JwtException ex) {
            return Optional.empty();
        }
    }
}
