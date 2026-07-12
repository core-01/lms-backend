package com.nile.lms.integration;

import com.nile.lms.config.OicProperties;
import com.nile.lms.dto.OicRegisterRequest;
import com.nile.lms.dto.OicRegisterResponse;
import com.nile.lms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class OicClient {

    private final WebClient oicWebClient;
    private final OicProperties oicProperties;

    public OicClient(WebClient oicWebClient, OicProperties oicProperties) {
        this.oicWebClient = oicWebClient;
        this.oicProperties = oicProperties;
    }

    public OicRegisterResponse register(OicRegisterRequest request) {
        try {
            return oicWebClient.post()
                    .uri(oicProperties.getRegisterUrl())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OicRegisterResponse.class)
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .block();
        } catch (WebClientResponseException.Conflict ex) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
        } catch (WebClientResponseException ex) {
            System.err.println("OIC call failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR",
                    "Registration failed, please try again");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            System.err.println("OIC call failed unexpectedly: " + ex.getClass().getName() + " - " + ex.getMessage());
            System.err.println("Root cause: " + cause.getClass().getName() + " - " + cause.getMessage());
            if (cause instanceof WebClientResponseException wcre) {
                System.err.println("Status: " + wcre.getStatusCode() + " Body: " + wcre.getResponseBodyAsString());
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR",
                    "Registration failed, please try again");
        }
    }
}