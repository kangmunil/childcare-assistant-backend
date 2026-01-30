package com.childcare.domain.ai.service;

import com.childcare.domain.ai.dto.AiChatRequest;
import com.childcare.domain.ai.dto.AiChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class AiService {

    private final WebClient aiWebClient;

    public AiService(WebClient webClient, @Value("${ai.service-url}") String aiServiceUrl) {
        this.aiWebClient = webClient.mutate()
                .baseUrl(aiServiceUrl)
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        log.info("Sending request to AI Service: {}, session: {}", request.getMessage(), request.getSessionId());

        return aiWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/chat").build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("AI Service returned error: {}", response.statusCode());
                    return response.createException();
                })
                .bodyToMono(AiChatResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying AI service call... (attempt {})", retrySignal.totalRetries() + 1)))
                .block(); // Blocking for now as the rest of the app seems synchronous
    }
}
