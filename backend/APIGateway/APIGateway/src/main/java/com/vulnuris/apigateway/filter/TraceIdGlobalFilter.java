package com.vulnuris.apigateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_ATTRIBUTE = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;
        ServerHttpRequest request = exchange.getRequest().mutate().header(TRACE_HEADER, finalTraceId).build();
        ServerWebExchange updatedExchange = exchange.mutate().request(request).build();
        updatedExchange.getAttributes().put(TRACE_ATTRIBUTE, finalTraceId);
        updatedExchange.getResponse().beforeCommit(() -> {
            updatedExchange.getResponse().getHeaders().set(TRACE_HEADER, finalTraceId);
            return Mono.empty();
        });

        return chain.filter(updatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
