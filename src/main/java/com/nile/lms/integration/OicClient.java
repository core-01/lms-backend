package com.nile.lms.integration;

import com.nile.lms.config.OicProperties;
import com.nile.lms.dto.OicRegisterRequest;
import com.nile.lms.dto.OicRegisterResponse;
import com.nile.lms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
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

        } catch (WebClientResponseException.Conflict ex) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");

        } catch (WebClientResponseException ex) {
            System.err.println("=== OIC HTTP FAILURE ===");
            System.err.println("STATUS: " + ex.getStatusCode());
            System.err.println("RESPONSE BODY: " + ex.getResponseBodyAsString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR", ex.getResponseBodyAsString());

        } catch (RuntimeException ex) {
            // Unwrap reactor's RetryExhaustedException to find the real cause
            Throwable real = Exceptions.unwrap(ex);
            while (real.getCause() != null && real != real.getCause()) {
                real = real.getCause();
            }

            System.err.println("=== OIC AUTH/UNKNOWN FAILURE ===");
            System.err.println("EXCEPTION TYPE: " + real.getClass().getName());
            System.err.println("MESSAGE: " + real.getMessage());

            if (real instanceof OAuth2AuthorizationException oauthEx) {
                System.err.println("OAUTH2 ERROR CODE: " + oauthEx.getError().getErrorCode());
                System.err.println("OAUTH2 ERROR DESCRIPTION: " + oauthEx.getError().getDescription());
                System.err.println("OAUTH2 ERROR URI: " + oauthEx.getError().getUri());
            }

            real.printStackTrace();

            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OIC_ERROR",
                    real.getMessage() != null ? real.getMessage() : "Registration failed, please try again");
        }
    }
}