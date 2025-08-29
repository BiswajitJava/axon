package com.axon.service.impl;

import com.axon.model.GitModule;
import com.axon.model.Lesson;
import com.axon.service.api.AiTutorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiTutorServiceImpl implements AiTutorService {
    // ... (existing fields remain the same)
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

    // ... (generateModule and answerQuestion methods remain the same)
    @Override
    public GitModule generateModule(String moduleKey) {
        System.out.println("Sending request to Fireworks AI for module: " + moduleKey + "...");
        String prompt = modulePrompts.get(moduleKey);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown module key: " + moduleKey);
        }
        // This reuses the new generic method to make the call
        return executeAiQuery(prompt, 5000);
    }

    @Override
    public String answerQuestion(String question) {
        System.out.println("Sending user question to Fireworks AI...");
        String prompt = buildQuestionPrompt(question);
        String rawApiResponse = executeAiQueryForText(prompt, 1024);
        return rawApiResponse;
    }


    @Override
    public GitModule generateMoreLessons(String moduleKey, List<Lesson> existingLessons) {
        System.out.println("Sending request to AI for more lessons on module: " + moduleKey + "...");
        String prompt = buildMoreLessonsPrompt(moduleKey, existingLessons);
        // We're asking for fewer lessons this time, so max_tokens can be lower
        return executeAiQuery(prompt, 2000);
    }

    private GitModule executeAiQuery(String prompt, int maxTokens) {
        ObjectNode payload = createPayload(prompt, maxTokens, 0.0);
        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Internal error: Failed to create JSON payload", e);
        }

        Request request = buildRequest(requestBodyJson);

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
            throw new RuntimeException("Could not process AI response: " + e.getMessage(), e);
        }
    }

    private String executeAiQueryForText(String prompt, int maxTokens) {
        ObjectNode payload = createPayload(prompt, maxTokens, 0.1);
        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Internal error: Failed to create JSON payload", e);
        }

        Request request = buildRequest(requestBodyJson);

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
            throw new RuntimeException("Could not get answer from AI: " + e.getMessage(), e);
        }
    }

    private ObjectNode createPayload(String prompt, int maxTokens, double temperature) {
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
        return payload;
    }

    private Request buildRequest(String jsonPayload) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        return new Request.Builder()
                .url(this.apiUrl)
                .header("Authorization", "Bearer " + this.apiToken)
                .post(body)
                .build();
    }

    // ... (getAvailableModules, extractJson, buildBasePrompt, and initializePrompts are unchanged)

    private String buildMoreLessonsPrompt(String moduleKey, List<Lesson> existingLessons) {
        // Extract the core topic from the module key, e.g., "basics" -> "the absolute basics of Git"
        String topic = getAvailableModules().get(moduleKey).replace("Git ", "").replace("Mastering ", "").replace("Working with ", "").replace("Inspecting and Rewriting ", "");

        // Collect all the commands that have already been taught
        String completedCommands = existingLessons.stream()
                .map(Lesson::command)
                .map(cmd -> "`" + cmd + "`") // Wrap in backticks for clarity
                .collect(Collectors.joining(", "));

        String basePrompt = String.format("""
        You are a curriculum generation bot. Your only function is to output a single, valid JSON object. Do not output any other text or markdown.
        
        Generate a new curriculum with 15 more advanced lessons for a developer learning about '%s'.
        
        CRITICAL RULE: The user has already learned the following commands. You MUST NOT create lessons for any of these commands or their basic variations: %s.
        
        Your goal is to introduce NEW, more advanced, or related commands and concepts that build upon the user's existing knowledge.
        
        Your output MUST be a single JSON object with a "moduleName" and a "lessons" array of exactly 15 lesson objects.
        Each lesson object MUST contain "title", "concept", "command", and "example_output".
        Inside "example_output", use these exact XML-style tags for colorization: <branch>...</branch>, <file>...</file>, <commit>...</commit>.
        
        Output only the raw JSON.
        """, topic, completedCommands);

        String systemPrompt = "<|im_start|>system\nYou are a helpful assistant that only outputs valid JSON.<|im_end|>\n";
        String userTemplate = "<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n";

        return systemPrompt + String.format(userTemplate, basePrompt);
    }

    // other existing private methods...
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

    private String buildQuestionPrompt(String question) {
        String systemPrompt = "<|im_start|>system\nYou are an expert Git tutor. Your goal is to help a user who is stuck. Provide clear, concise, and helpful explanations. Use markdown for code blocks and emphasis.<|im_end|>\n";
        String userTemplate = "<|im_start|>user\nI'm having trouble and have a question: \"%s\"\n\nPlease explain the concept or command, provide a correct example, and if applicable, explain what might be going wrong for me.<|im_end|>\n<|im_start|>assistant\n";
        return systemPrompt + String.format(userTemplate, question);
    }
}