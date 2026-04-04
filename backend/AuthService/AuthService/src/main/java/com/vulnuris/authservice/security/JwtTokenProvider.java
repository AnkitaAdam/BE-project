package com.vulnuris.authservice.security;

import com.vulnuris.authservice.entity.User;
import com.vulnuris.authservice.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return generateToken(user.getUsername(), roles, "access", jwtProperties.getAccessTokenValiditySeconds());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user.getUsername(), List.of(), "refresh", jwtProperties.getRefreshTokenValiditySeconds());
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Set<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        Object rawRoles = claims.get(CLAIM_ROLES);
        if (!(rawRoles instanceof Collection<?> values)) {
            return Set.of();
        }

        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isAccessToken(String token) {
        return "access".equalsIgnoreCase(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equalsIgnoreCase(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Claims parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtProperties.signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String issuer = claims.getIssuer();
            if (!jwtProperties.getIssuer().equals(issuer)) {
                throw new InvalidTokenException("Token issuer does not match expected issuer");
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    private String generateToken(String subject, Collection<String> roles, String tokenType, long ttlSeconds) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(jwtProperties.signingKey())
                .compact();
    }
}
