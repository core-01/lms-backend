package com.nile.lms.service;

import com.nile.lms.dto.OicRegisterRequest;
import com.nile.lms.dto.OicRegisterResponse;
import com.nile.lms.dto.request.RegisterRequest;
import com.nile.lms.dto.response.RegisterResponse;
import com.nile.lms.exception.ApiException;
import com.nile.lms.integration.OicClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final OicClient oicClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(OicClient oicClient) {
        this.oicClient = oicClient;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match");
        }

        String hash = passwordEncoder.encode(request.password());

        OicRegisterResponse oicResponse = oicClient.register(
                new OicRegisterRequest(request.fullName(), request.email(), request.phoneNumber(), hash)
        );

        return new RegisterResponse(oicResponse.userId(), oicResponse.status(), "Account created successfully");
    }
}