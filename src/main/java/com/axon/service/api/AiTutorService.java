package com.axon.service.api;

import com.axon.model.GitModule;

import java.util.Map;

/**
 * Service interface for interacting with the AI to generate educational content.
 * This defines the contract for what an AI Tutor can do.
 */
public interface AiTutorService {

    /**
     * Generates a complete learning module for a given topic key.
     *
     * @param moduleKey The unique identifier for the module (e.g., "basics").
     * @return A GitModule object containing the full curriculum.
     * @throws IllegalArgumentException if the moduleKey is unknown.
     * @throws RuntimeException if the AI service call or parsing fails.
     */
    GitModule generateModule(String moduleKey);

    /**
     * Retrieves a map of all available module keys and their user-friendly names.
     *
     * @return A Map where the key is the module identifier and the value is the display name.
     */
    Map<String, String> getAvailableModules();
}