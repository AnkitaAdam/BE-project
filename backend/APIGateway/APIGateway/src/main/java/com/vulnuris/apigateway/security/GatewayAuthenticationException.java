package com.vulnuris.apigateway.security;

public class GatewayAuthenticationException extends RuntimeException {

    public GatewayAuthenticationException(String message) {
        super(message);
    }
}
