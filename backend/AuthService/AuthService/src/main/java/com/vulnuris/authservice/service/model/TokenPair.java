package com.vulnuris.authservice.service.model;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
