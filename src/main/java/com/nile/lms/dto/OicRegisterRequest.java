package com.nile.lms.dto;

public record OicRegisterRequest(
        String fullName,
        String email,
        String phoneNumber,
        String passwordHash
) {}