package com.axon.service.impl;

import com.axon.model.LearningModule;
import com.axon.service.api.AiTutorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiToken;

    public AiTutorServiceImpl(ObjectMapper objectMapper,
                              @Value("${app.ai.api-url}") String apiUrl,
                              @Value("${app.fireworks.api-key}") String apiToken) {
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public LearningModule generateModuleFromPrompt(String prompt, int maxTokens) {
        String rawApiResponse = executeAiQuery(prompt, maxTokens, 0.0);
        String cleanJson = extractJson(rawApiResponse);
        try {
            return objectMapper.readValue(cleanJson, LearningModule.class);
        } catch (Exception e) {
            System.err.println("Failed to parse the following JSON:\n" + cleanJson);
            throw new RuntimeException("Failed to parse AI module response.", e);
        }
    }

    @Override
    public String answerQuestionFromPrompt(String prompt, int maxTokens) {
        return executeAiQuery(prompt, maxTokens, 0.1);
    }

    private String executeAiQuery(String prompt, int maxTokens, double temperature) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", "accounts/fireworks/models/qwen3-coder-30b-a3b-instruct");

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        payload.set("messages", messages);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Internal error: Failed to create JSON payload", e);
        }

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBodyJson, JSON);
        Request request = new Request.Builder()
                .url(this.apiUrl)
                .header("Authorization", "Bearer " + this.apiToken)
                .post(body)
                .build();

        String rawApiResponseForDebugging = "";
        try (Response response = httpClient.newCall(request).execute()) {
            rawApiResponseForDebugging = response.body() != null ? response.body().string() : "No response body";
            if (!response.isSuccessful()) {
                throw new RuntimeException("API call failed with code " + response.code() + ": " + rawApiResponseForDebugging);
            }
            JsonNode responseNode = objectMapper.readTree(rawApiResponseForDebugging);
            return responseNode.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            System.err.println("\n--- RAW API RESPONSE ---");
            System.err.println(rawApiResponseForDebugging);
            System.err.println("--- END RAW RESPONSE ---\n");
            throw new RuntimeException("Could not get a response from the AI: " + e.getMessage(), e);
        }
    }

    private String extractJson(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        throw new RuntimeException("Could not find valid JSON in the AI output.");
    }
}