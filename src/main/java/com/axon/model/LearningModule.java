package com.axon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections; // Import Collections
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LearningModule(String moduleName, List<Lesson> lessons) {

    /**
     * Compact constructor to ensure the lessons list is never null.
     * If the incoming list from the JSON is null, it will be replaced
     * with an unmodifiable empty list, preventing NullPointerExceptions.
     */
    public LearningModule {
        if (lessons == null) {
            lessons = Collections.emptyList();
        }
    }
}