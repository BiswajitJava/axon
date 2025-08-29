package com.axon.service.api;

import com.axon.model.Lesson;
import java.util.List;
import java.util.Map;

/**
 * A service for generating technology-specific prompts for the AI.
 */
public interface PromptService {
    Map<String, String> getAvailableModules();
    String getTechnologyName();
    String buildInitialModulePrompt(String moduleKey);
    String buildMoreLessonsPrompt(String moduleKey, List<Lesson> existingLessons);
    String buildQuestionPrompt(String question);

    /**
     * NEW METHOD: Builds a prompt to ask the AI for a summary of a completed module.
     *
     * @param moduleName The display name of the module (e.g., "Git Basics: The First Steps").
     * @param lessons The list of all lessons completed in the module.
     * @return A formatted prompt string to be sent to the AI.
     */
    String buildSummaryPrompt(String moduleName, List<Lesson> lessons);
}