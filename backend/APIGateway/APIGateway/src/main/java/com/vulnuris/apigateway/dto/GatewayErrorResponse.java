package com.vulnuris.apigateway.dto;

import java.time.Instant;

public record GatewayErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId
) {
}
