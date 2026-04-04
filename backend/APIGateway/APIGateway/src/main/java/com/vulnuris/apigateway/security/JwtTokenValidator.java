package com.vulnuris.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    private final GatewayJwtProperties jwtProperties;

    public Claims parseAndValidateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtProperties.signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
                throw new GatewayAuthenticationException("Token issuer mismatch");
            }
            if (!"access".equalsIgnoreCase(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                throw new GatewayAuthenticationException("Access token is required for this endpoint");
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new GatewayAuthenticationException("Invalid or expired access token");
        }
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public Set<String> getRoles(Claims claims) {
        Object rawRoles = claims.get(CLAIM_ROLES);
        if (!(rawRoles instanceof Collection<?> collection)) {
            return Set.of();
        }

        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
