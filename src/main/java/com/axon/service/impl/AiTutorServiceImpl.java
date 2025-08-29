package com.axon.service.impl;

import com.axon.model.GitModule;
import com.axon.service.api.AiTutorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> modulePrompts;
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
        this.modulePrompts = initializePrompts();
    }

    @Override
    public GitModule generateModule(String moduleKey) {
        System.out.println("Sending request to Fireworks AI for module: " + moduleKey + "...");
        String prompt = modulePrompts.get(moduleKey);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown module key: " + moduleKey);
        }

        ObjectNode payload = objectMapper.createObjectNode();
        // ========================================================================
        // THE FINAL FIX: Using the powerful Qwen Coder model you selected
        // ========================================================================
        payload.put("model", "accounts/fireworks/models/qwen3-coder-30b-a3b-instruct");

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        payload.set("messages", messages);
        payload.put("max_tokens", 5000);
        payload.put("temperature", 0.0); // Use 0.0 for maximum determinism with coder models

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
            String generatedText = responseNode.path("choices").get(0).path("message").path("content").asText();

            String cleanJson = extractJson(generatedText);
            return objectMapper.readValue(cleanJson, GitModule.class);

        } catch (Exception e) {
            System.err.println("\n--- RAW API RESPONSE ---");
            System.err.println(rawApiResponseForDebugging);
            System.err.println("--- END RAW RESPONSE ---\n");
            throw new RuntimeException("Could not start module: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getAvailableModules() {
        return Map.of(
                "basics", "Git Basics: The First Steps",
                "branching", "Mastering Git Branching",
                "remotes", "Working with Remote Repositories",
                "history", "Inspecting and Rewriting History"
        );
    }

    private String extractJson(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        if (firstBrace != -1) {
            return text.substring(firstBrace);
        }
        throw new RuntimeException("Could not find valid JSON in the AI output.");
    }

    private String buildBasePrompt(String topic, int lessonCount) {
        // This forceful prompt works perfectly with powerful instruction-following models.
        return String.format("""
        You are a curriculum generation bot. Your only function is to output a single, valid JSON object. Do not output any other text, greetings, apologies, or markdown formatting. Do not use a "think" block.
        
        Generate a curriculum for a developer learning about '%s'.
        
        Your output MUST be a single JSON object.
        The root object must have a key "moduleName" (string) and a key "lessons" (array).
        The "lessons" array must contain exactly %d lesson objects.
        Each lesson object MUST contain exactly four keys: "title" (string), "concept" (string), "command" (string), and "example_output" (string).
        
        CRITICAL: Inside the "example_output" string, you MUST add specific XML-style tags around key terms for colorization.
        YOU MUST USE THESE EXACT TAGS:
        - Wrap branch names in <branch>...</branch>.
        - Wrap filenames and paths in <file>...</file>.
        - Wrap commit hashes in <commit>...</commit>.
        
        EXAMPLE of a perfect "example_output": "On branch <branch>master</branch>\\nYour branch is up to date with <file>'origin/master'</file>."
        
        Output only the raw JSON.
        """, topic, lessonCount);
    }

    // UPDATED: This method now uses the specific "ChatML" prompt format for Qwen models.
    private Map<String, String> initializePrompts() {
        String systemPrompt = "<|im_start|>system\nYou are a helpful assistant that only outputs valid JSON.<|im_end|>\n";
        String userTemplate = "<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n";

        return Map.of(
                "basics", systemPrompt + String.format(userTemplate, buildBasePrompt("the absolute basics of Git, covering init, add, commit, status, and log", 30)),
                "branching", systemPrompt + String.format(userTemplate, buildBasePrompt("Git branching, covering create, switch, merge, and delete branches", 30)),
                "remotes", systemPrompt + String.format(userTemplate, buildBasePrompt("working with remote Git repositories, covering clone, push, pull, and fetch", 30)),
                "history", systemPrompt + String.format(userTemplate, buildBasePrompt("inspecting and rewriting Git history, covering rebase, amend, and reset", 30))
        );
    }
}