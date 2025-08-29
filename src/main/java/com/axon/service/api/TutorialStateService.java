package com.axon.service.api;

import com.axon.model.Lesson;

import java.util.Optional;

/**
 * Service interface for managing the user's learning progress.
 * This defines the contract for stateful tutorial management.
 */
public interface TutorialStateService {

    /**
     * Starts a new learning module, resetting any previous progress.
     *
     * @param moduleKey The key of the module to start.
     */
    void startModule(String moduleKey);

    /**
     * Retrieves the current lesson based on the user's progress.
     *
     * @return An Optional containing the current Lesson, or an empty Optional if the module is complete.
     */
    Optional<Lesson> getCurrentLesson();

    /**
     * Advances the user to the next lesson in the current module.
     *
     * @return An Optional containing the next Lesson, or an empty Optional if the module is complete.
     */
    Optional<Lesson> getNextLesson();

    /**
     * Gets a human-readable string describing the user's current progress.
     *
     * @return A status string.
     */
    String getStatus();
}
