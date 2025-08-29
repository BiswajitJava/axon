package com.axon.service.api;

import com.axon.model.LearningModule;

/**
 * A generic service for interacting with the AI model to generate content.
 */
public interface AiTutorService {

    /**
     * Executes a query to the AI to generate a learning module from a specific prompt.
     *
     * @param prompt The complete, formatted prompt to send to the AI.
     * @param maxTokens The maximum number of tokens for the response.
     * @return A LearningModule object parsed from the AI's JSON output.
     */
    LearningModule generateModuleFromPrompt(String prompt, int maxTokens);

    /**
     * Executes a query to the AI to get a text-based answer to a question.
     *
     * @param prompt The complete, formatted prompt containing the user's question.
     * @param maxTokens The maximum number of tokens for the response.
     * @return A raw string response from the AI.
     */
    String answerQuestionFromPrompt(String prompt, int maxTokens);
}