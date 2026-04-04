package com.vulnuris.authservice.dto.response;

public record AuthTokensResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        UserResponse user
) {
}
