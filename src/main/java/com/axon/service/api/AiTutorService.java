package com.axon.service.api;

import com.axon.model.GitModule;
import com.axon.model.Lesson;

import java.util.List;
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
     */
    GitModule generateModule(String moduleKey);

    /**
     * Retrieves a map of all available module keys and their user-friendly names.
     *
     * @return A Map where the key is the module identifier and the value is the display name.
     */
    Map<String, String> getAvailableModules();

    /**
     * Generates an answer to a user's question about a command or concept.
     *
     * @param question The user's question as a string.
     * @return A string containing the AI's explanation.
     */
    String answerQuestion(String question);

    /**
     * Generates additional lessons for a topic, avoiding concepts that have already been taught.
     *
     * @param moduleKey The key of the current module (e.g., "basics").
     * @param existingLessons A list of lessons the user has already completed.
     * @return A new GitModule containing fresh, non-repeating lessons.
     */
    GitModule generateMoreLessons(String moduleKey, List<Lesson> existingLessons);
}