package com.axon.service.api;

import com.axon.model.Lesson;
import java.util.List; // Required for getCurrentModuleLessons
import java.util.Map;
import java.util.Optional;

public interface TutorialStateService {
    void startModule(String technology, String moduleKey);
    Optional<Lesson> getCurrentLesson();
    Optional<Lesson> getNextLesson();
    String getStatus();
    boolean isModuleComplete();
    void appendMoreLessons();
    String answerQuestion(String question);
    Map<String, String> getAvailableModulesForCurrentTechnology();

    // --- NEW METHODS FOR NAVIGATION AND SUMMARY ---

    /**
     * Moves the progress to the previous lesson in the current module.
     *
     * @return An Optional containing the previous lesson, or empty if on the first lesson.
     */
    Optional<Lesson> getPreviousLesson();

    /**
     * Jumps the progress to a specific lesson number in the current module.
     *
     * @param lessonNumber The 1-based lesson number to jump to.
     * @return An Optional containing the lesson at that number, or empty if the number is invalid.
     */
    Optional<Lesson> goToLesson(int lessonNumber);

    /**
     * Retrieves the complete list of lessons for the currently active module.
     *
     * @return A List of all Lessons, or an empty list if no module is active.
     */
    List<Lesson> getCurrentModuleLessons();

    /**
     * Generates a text summary of the current module by querying the AI.
     *
     * @return A string containing the formatted summary.
     * @throws IllegalStateException if the module is not yet complete.
     */
    String generateSummary();
}