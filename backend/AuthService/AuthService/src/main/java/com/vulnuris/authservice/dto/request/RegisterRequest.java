package com.vulnuris.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Username may only contain letters, numbers, dot, underscore, and hyphen"
        )
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password
) {
}
