package com.example.productai.service;

import com.example.productai.dto.HeliumKeywordRow;
import com.example.productai.dto.qwen.ChatCompletionRequest;
import com.example.productai.dto.qwen.ChatCompletionResponse;
import com.example.productai.dto.qwen.Message;
import com.example.productai.util.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class QwenClient {
    private final WebClient webClient;
    private final RetryTemplate retryTemplate;
    private final String model;
    private final double temperature;
    private final long timeoutSeconds;

    public QwenClient(@Value("${qwen.base-url}") String baseUrl,
                      @Value("${qwen.api-key}") String apiKey,
                      @Value("${qwen.model}") String model,
                      @Value("${qwen.temperature}") double temperature,
                      @Value("${qwen.timeout-seconds}") long timeoutSeconds,
                      RetryTemplate qwenRetryTemplate) {
        this.model = model;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.retryTemplate = qwenRetryTemplate;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String analyzeKeyword(HeliumKeywordRow row, Integer localScore) {
        return retryTemplate.execute(context -> doAnalyzeKeyword(row, localScore));
    }

    private String doAnalyzeKeyword(HeliumKeywordRow row, Integer localScore) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(model);
        request.setTemperature(temperature);
        request.setMessages(List.of(
                new Message("system", "You are a strict Amazon keyword opportunity evaluator."),
                new Message("user", buildPrompt(row, localScore))
        ));

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty() || response.getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Empty response from Qwen");
        }
        return response.getChoices().get(0).getMessage().getContent();
    }

    private String buildPrompt(HeliumKeywordRow row, Integer localScore) {
        return """
            You are an Amazon FBA product research expert.

            Your job is to evaluate if this keyword can represent a good winning product opportunity.

            Return ONLY valid JSON with this exact format:
            {
              "score": 0,
              "decision": "KEEP",
              "explanation": "short explanation"
            }

            Strict rules:
            - score must be an integer from 0 to 100
            - decision must be KEEP or REJECT
            - explanation maximum 20 words
            - never return anything outside JSON

            Mandatory rejection rules:
            - REJECT if the keyword is a brand, trademark, or contains a brand name
            - REJECT if the product is food, drink, supplement, vitamin, nutrition, edible, or grocery related
            - REJECT if the product is health, medical, pharmaceutical, hygiene-sensitive, body treatment, or safety-regulated
            - REJECT if the product is difficult to differentiate
            - REJECT if competition is too high
            - REJECT if title density is too high
            - REJECT if CPR is too high
            - REJECT if search demand is too low

            KEEP only if:
            - product looks generic and non-branded
            - product is not in food or health categories
            - search volume is strong
            - competition is acceptable or low
            - title density is low or reasonable
            - CPR is low or reasonable
            - demand/competition ratio looks promising
            - product seems easy to source and sell on Amazon

            Scoring guidance:
            - High score: strong demand, low competition, low title density, low CPR, generic product, easy entry
            - Medium score: some opportunity but moderate competition
            - Low score: branded, restricted, risky, saturated, weak demand, or hard to launch
            - consider localScore as a helpful pre-score, not the final answer

            Important:
            - Be very strict about branded keywords
            - Be very strict about food and health products
            - Prefer simple physical products, evergreen products, non-regulated products
            - Prefer products with organic ranking opportunity

            Keyword data:
            Keyword Phrase: %s
            Keyword Sales: %s
            Cerebro IQ Score: %s
            Search Volume: %s
            Search Volume Trend: %s
            H10 PPC Suggested Bid: %s
            Competing Products: %s
            CPR: %s
            Organic: %s
            Title Density: %s
            Smart Complete: %s
            Amazon Recommended: %s
            Local Score: %s
            """.formatted(
                safe(row.getKeywordPhrase()),
                NumberUtils.toInteger(row.getKeywordSales()),
                NumberUtils.toDouble(row.getCerebroIqScore()),
                NumberUtils.toInteger(row.getSearchVolume()),
                safe(row.getSearchVolumeTrend()),
                NumberUtils.toDouble(row.getH10PpcSuggBid()),
                NumberUtils.toInteger(row.getCompetingProducts()),
                NumberUtils.toInteger(row.getCpr()),
                NumberUtils.toInteger(row.getOrganic()),
                NumberUtils.toInteger(row.getTitleDensity()),
                safe(row.getSmartComplete()),
                safe(row.getAmazonRecommended()),
                localScore
        );
    }

    private String safe(String value) { return value == null ? "" : value; }
}
