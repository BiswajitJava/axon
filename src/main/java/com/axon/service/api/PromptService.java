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
}