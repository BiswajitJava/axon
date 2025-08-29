package com.axon.service.impl;

import com.axon.model.LearningModule;
import com.axon.model.Lesson;
import com.axon.service.api.AiTutorService;
import com.axon.service.api.PromptService;
import com.axon.service.api.TutorialStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TutorialStateServiceImpl implements TutorialStateService {

    public record Progress(String currentTechnology, String currentModuleKey, int currentLessonIndex) {}
    private static final Path PROGRESS_FILE = Path.of(System.getProperty("user.home"), ".axon-progress.json");

    private final ObjectMapper objectMapper;
    private final AiTutorService aiTutorService;
    private final ApplicationContext context;

    private LearningModule currentModule;
    private Progress currentProgress;
    private PromptService currentPromptService;

    public TutorialStateServiceImpl(ObjectMapper objectMapper, AiTutorService aiTutorService, ApplicationContext context) {
        this.objectMapper = objectMapper;
        this.aiTutorService = aiTutorService;
        this.context = context;
    }

    @PostConstruct
    public void loadProgress() {
        if (Files.exists(PROGRESS_FILE)) {
            try {
                this.currentProgress = objectMapper.readValue(PROGRESS_FILE.toFile(), Progress.class);
                if (this.currentProgress != null) {
                    System.out.println("Resuming previous session for " + currentProgress.currentTechnology() + "...");
                    this.currentPromptService = getPromptServiceFor(currentProgress.currentTechnology());
                    String prompt = currentPromptService.buildInitialModulePrompt(currentProgress.currentModuleKey());
                    this.currentModule = aiTutorService.generateModuleFromPrompt(prompt, 5000);
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load progress file. " + e.getMessage());
                this.currentProgress = null;
            }
        }
    }

    @Override
    public void startModule(String technology, String moduleKey) {
        this.currentPromptService = getPromptServiceFor(technology);
        if (!currentPromptService.getAvailableModules().containsKey(moduleKey)) {
            throw new IllegalArgumentException("Unknown module key '" + moduleKey + "' for " + technology);
        }
        String prompt = currentPromptService.buildInitialModulePrompt(moduleKey);
        this.currentModule = aiTutorService.generateModuleFromPrompt(prompt, 5000);
        this.currentProgress = new Progress(technology, moduleKey, 0);
        saveProgress();
    }

    @Override
    public Optional<Lesson> getCurrentLesson() {
        if (currentModule == null || currentProgress == null || isModuleComplete()) {
            return Optional.empty();
        }
        return Optional.of(currentModule.lessons().get(currentProgress.currentLessonIndex()));
    }

    @Override
    public Optional<Lesson> getNextLesson() {
        if (currentModule == null) {
            return Optional.empty();
        }
        currentProgress = new Progress(currentProgress.currentTechnology(), currentProgress.currentModuleKey(), currentProgress.currentLessonIndex() + 1);
        saveProgress();
        return getCurrentLesson();
    }

    @Override
    public String getStatus() {
        if (currentProgress == null || currentPromptService == null) {
            return "No tutorial in progress. Use 'git start' or 'docker start'.";
        }
        if (isModuleComplete()) {
            return String.format("You have completed all %d lessons of the '%s' module for %s!",
                    currentModule.lessons().size(), currentProgress.currentModuleKey(), currentPromptService.getTechnologyName());
        }
        return String.format("Technology: %s | Module: '%s' | Lesson %d of %d.",
                currentPromptService.getTechnologyName(),
                currentProgress.currentModuleKey(),
                currentProgress.currentLessonIndex() + 1,
                currentModule.lessons().size());
    }

    @Override
    public boolean isModuleComplete() {
        if (currentModule == null || currentProgress == null) {
            return false;
        }
        return currentProgress.currentLessonIndex() >= currentModule.lessons().size();
    }

    @Override
    public void appendMoreLessons() {
        if (!isModuleComplete()) throw new IllegalStateException("Finish current lessons first.");
        if (currentModule == null) throw new IllegalStateException("No active module.");

        String prompt = currentPromptService.buildMoreLessonsPrompt(currentProgress.currentModuleKey(), currentModule.lessons());
        LearningModule newLessonsModule = aiTutorService.generateModuleFromPrompt(prompt, 2500);

        List<Lesson> combinedLessons = new ArrayList<>(currentModule.lessons());
        combinedLessons.addAll(newLessonsModule.lessons());
        this.currentModule = new LearningModule(currentModule.moduleName(), combinedLessons);
        saveProgress();
    }

    @Override
    public String answerQuestion(String question) {
        if (currentPromptService == null) {
            throw new IllegalStateException("Cannot answer question without context. Please start a module first (e.g., 'git start basics').");
        }
        String prompt = currentPromptService.buildQuestionPrompt(question);
        return aiTutorService.answerQuestionFromPrompt(prompt, 2500);
    }

    @Override
    public Map<String, String> getAvailableModulesForCurrentTechnology() {
        if (currentPromptService == null) {
            return Map.of();
        }
        return currentPromptService.getAvailableModules();
    }

    private void saveProgress() {
        try {
            objectMapper.writeValue(PROGRESS_FILE.toFile(), currentProgress);
        } catch (IOException e) {
            System.err.println("Warning: Could not save progress: " + e.getMessage());
        }
    }

    private PromptService getPromptServiceFor(String technology) {
        String beanName = technology.toLowerCase() + "PromptService";
        return context.getBean(beanName, PromptService.class);
    }
}