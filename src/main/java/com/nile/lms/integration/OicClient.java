package com.nile.lms.integration;

import com.nile.lms.config.OicProperties;
import com.nile.lms.dto.OicRegisterRequest;
import com.nile.lms.dto.OicRegisterResponse;
import com.nile.lms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
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
                    .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction
                            .clientRegistrationId("oic"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OicRegisterResponse.class)
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .block();

        } catch (RuntimeException ex) {
            Throwable real = Exceptions.unwrap(ex);
            while (real.getCause() != null && real != real.getCause()) {
                real = real.getCause();
            }

            System.err.println("=== OIC FAILURE ===");
            System.err.println("EXCEPTION TYPE: " + real.getClass().getName());
            System.err.println("MESSAGE: " + real.getMessage());

            if (real instanceof WebClientResponseException wcre) {
                System.err.println("STATUS: " + wcre.getStatusCode());
                System.err.println("RESPONSE HEADERS: " + wcre.getHeaders());
                System.err.println("RESPONSE BODY: " + wcre.getResponseBodyAsString());

                if (wcre instanceof WebClientResponseException.Conflict) {
                    throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
                }
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR", wcre.getResponseBodyAsString());
            }

            real.printStackTrace();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR",
                    real.getMessage() != null ? real.getMessage() : "Registration failed, please try again");
        }
    }
}