package com.vulnuris.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password
) {
}
