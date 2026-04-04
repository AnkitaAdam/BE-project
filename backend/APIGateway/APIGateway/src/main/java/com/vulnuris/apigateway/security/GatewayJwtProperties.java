package com.vulnuris.apigateway.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import javax.crypto.SecretKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gateway.security.jwt")
@Validated
@Data
public class GatewayJwtProperties {

    @NotBlank(message = "JWT secret is required")
    @Size(min = 32, message = "JWT secret must be at least 32 characters")
    private String secret;

    @NotBlank(message = "Expected JWT issuer is required")
    private String issuer = "vulnuris-auth-service";

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
