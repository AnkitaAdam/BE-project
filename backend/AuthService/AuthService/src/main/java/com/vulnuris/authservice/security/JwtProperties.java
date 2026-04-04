package com.vulnuris.authservice.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import javax.crypto.SecretKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.jwt")
@Validated
@Data
public class JwtProperties {

    @NotBlank(message = "JWT secret is required")
    @Size(min = 32, message = "JWT secret must be at least 32 characters")
    private String secret;

    @NotBlank(message = "JWT issuer is required")
    private String issuer = "vulnuris-auth-service";

    @Min(value = 60, message = "Access token TTL must be at least 60 seconds")
    private long accessTokenValiditySeconds = 1800;

    @Min(value = 300, message = "Refresh token TTL must be at least 300 seconds")
    private long refreshTokenValiditySeconds = 2592000;

    public SecretKey signingKey() {
        byte[] keyBytes = secret.getBytes();
        if (isBase64Encoded(secret)) {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isBase64Encoded(String value) {
        return value.matches("^[A-Za-z0-9+/=]+$") && value.length() % 4 == 0;
    }
}
