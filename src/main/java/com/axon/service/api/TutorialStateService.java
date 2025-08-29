package com.axon.service.api;

import com.axon.model.Lesson;
import java.util.Optional;
import java.util.Map;

public interface TutorialStateService {
    void startModule(String technology, String moduleKey);
    Optional<Lesson> getCurrentLesson();
    Optional<Lesson> getNextLesson();
    String getStatus();
    boolean isModuleComplete();
    void appendMoreLessons();
    String answerQuestion(String question);
    Map<String, String> getAvailableModulesForCurrentTechnology();
}