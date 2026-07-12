package com.nile.lms.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "\\d{10}") String phoneNumber,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String confirmPassword
) {}