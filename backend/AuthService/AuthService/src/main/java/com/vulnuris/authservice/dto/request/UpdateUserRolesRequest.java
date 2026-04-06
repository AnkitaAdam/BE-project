package com.vulnuris.authservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateUserRolesRequest(
        @NotNull(message = "Roles are required")
        @NotEmpty(message = "At least one role is required")
        Set<String> roles
) {
}
