package com.axon.service.api;

import com.axon.model.Lesson;

import java.util.Optional;

/**
 * Service interface for managing the user's learning progress.
 * This defines the contract for stateful tutorial management.
 */
public interface TutorialStateService {

    // ... (existing methods are unchanged)
    void startModule(String moduleKey);
    Optional<Lesson> getCurrentLesson();
    Optional<Lesson> getNextLesson();
    String getStatus();

    /**
     * Checks if the user has completed all lessons in the current module.
     *
     * @return true if the module is active and all lessons have been viewed, false otherwise.
     */
    boolean isModuleComplete();

    /**
     * Generates more lessons for the current module and appends them to the lesson list.
     * Throws an exception if the module is not yet complete.
     */
    void appendMoreLessons();
}