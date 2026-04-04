package com.vulnuris.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.apigateway.dto.GatewayErrorResponse;
import com.vulnuris.apigateway.security.GatewayAuthenticationException;
import com.vulnuris.apigateway.security.JwtTokenValidator;
import com.vulnuris.apigateway.security.RouteRolePolicy;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_USER_HEADER = "X-Auth-User";
    private static final String AUTH_ROLES_HEADER = "X-Auth-Roles";

    private final JwtTokenValidator jwtTokenValidator;
    private final RouteRolePolicy routeRolePolicy;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (routeRolePolicy.isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtTokenValidator.parseAndValidateAccessToken(token);
        } catch (GatewayAuthenticationException ex) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        String username = jwtTokenValidator.getUsername(claims);
        Set<String> roles = jwtTokenValidator.getRoles(claims);

        if (username == null || username.isBlank()) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token subject is missing");
        }

        if (!routeRolePolicy.isAuthorized(path, method, roles)) {
            return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "Insufficient role for requested route");
        }

        ServerHttpRequest updatedRequest = exchange.getRequest().mutate()
                .header(AUTH_USER_HEADER, username)
                .header(AUTH_ROLES_HEADER, String.join(",", roles))
                .build();

        return chain.filter(exchange.mutate().request(updatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        String traceId = resolveTraceId(exchange);
        GatewayErrorResponse payload = new GatewayErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath(),
                traceId
        );

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            body = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                    + "\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(TraceIdGlobalFilter.TRACE_HEADER, traceId);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        Object traceIdAttr = exchange.getAttribute(TraceIdGlobalFilter.TRACE_ATTRIBUTE);
        if (traceIdAttr instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }

        String traceHeader = exchange.getRequest().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_HEADER);
        return traceHeader != null && !traceHeader.isBlank() ? traceHeader : "N/A";
    }
}
