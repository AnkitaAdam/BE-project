package com.vulnuris.apigateway.security;

import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class RouteRolePolicy {

    private static final Set<String> ALL_ROLES = Set.of("ADMIN", "ANALYST", "VIEWER");
    private static final Set<String> ANALYST_OR_ADMIN = Set.of("ADMIN", "ANALYST");
    private static final Set<String> ADMIN_ONLY = Set.of("ADMIN");

    public boolean isPublicEndpoint(String path, HttpMethod method) {
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        String normalizedPath = normalizePath(path);
        if ("/actuator/health".equals(normalizedPath) || "/actuator/info".equals(normalizedPath)) {
            return true;
        }

        return HttpMethod.POST.equals(method)
                && (
                "/api/auth/login".equals(normalizedPath)
                        || "/api/auth/register".equals(normalizedPath)
                        || "/api/auth/refresh".equals(normalizedPath)
        );
    }

    public boolean isAuthorized(String path, HttpMethod method, Set<String> userRoles) {
        Set<String> requiredRoles = requiredRoles(path, method);
        if (requiredRoles.isEmpty()) {
            return true;
        }

        return userRoles.stream().anyMatch(requiredRoles::contains);
    }

    private Set<String> requiredRoles(String path, HttpMethod method) {
        String normalizedPath = normalizePath(path);

        if (normalizedPath.startsWith("/api/auth/admin")) {
            return ADMIN_ONLY;
        }

        if (normalizedPath.startsWith("/api/auth")) {
            return ALL_ROLES;
        }

        if (normalizedPath.startsWith("/api/events")) {
            return HttpMethod.GET.equals(method) ? ALL_ROLES : ANALYST_OR_ADMIN;
        }

        if (normalizedPath.startsWith("/api/ingest") || normalizedPath.startsWith("/api/correlation")) {
            return ANALYST_OR_ADMIN;
        }

        if (normalizedPath.startsWith("/api/report")) {
            return ALL_ROLES;
        }

        return ANALYST_OR_ADMIN;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }
}
