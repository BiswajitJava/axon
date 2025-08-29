package com.axon.service.impl;
import com.axon.model.GitModule;
import com.axon.model.Lesson;
import com.axon.service.api.AiTutorService;
import com.axon.service.api.TutorialStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TutorialStateServiceImpl implements TutorialStateService {
    // ... (existing fields and constructor are unchanged)
    public record Progress(String currentModuleKey, int currentLessonIndex) {}
    private static final Path PROGRESS_FILE = Path.of(System.getProperty("user.home"), ".axon-progress.json");
    private final ObjectMapper objectMapper;
    private final AiTutorService aiTutorService;
    private GitModule currentModule;
    private Progress currentProgress;

    public TutorialStateServiceImpl(ObjectMapper objectMapper, AiTutorService aiTutorService) {
        this.objectMapper = objectMapper;
        this.aiTutorService = aiTutorService;
    }

    // ... (loadProgress, startModule, getCurrentLesson, getNextLesson, getStatus, saveProgress are unchanged)
    @PostConstruct
    public void loadProgress() {
        if (Files.exists(PROGRESS_FILE)) {
            try {
                this.currentProgress = objectMapper.readValue(PROGRESS_FILE.toFile(), Progress.class);
                if (this.currentProgress != null) {
                    System.out.println("Resuming previous session...");
                    this.currentModule = aiTutorService.generateModule(this.currentProgress.currentModuleKey());
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load progress file.");
                this.currentProgress = null;
            }
        }
    }

    @Override
    public void startModule(String moduleKey) {
        this.currentModule = aiTutorService.generateModule(moduleKey);
        this.currentProgress = new Progress(moduleKey, 0);
        saveProgress();
    }

    @Override
    public Optional<Lesson> getCurrentLesson() {
        if (currentModule == null || currentProgress == null || currentProgress.currentLessonIndex() >= currentModule.lessons().size()) {
            return Optional.empty();
        }
        return Optional.of(currentModule.lessons().get(currentProgress.currentLessonIndex()));
    }

    @Override
    public Optional<Lesson> getNextLesson() {
        if (currentModule == null) {
            System.out.println("No active module. Use 'start [key]' to begin.");
            return Optional.empty();
        }
        currentProgress = new Progress(currentProgress.currentModuleKey(), currentProgress.currentLessonIndex() + 1);
        saveProgress();
        return getCurrentLesson();
    }

    @Override
    public String getStatus() {
        if (currentProgress == null) {
            return "No tutorial in progress.";
        }
        // Add a check for module completion in the status
        if (isModuleComplete()) {
            return String.format("You have completed all %d lessons of the '%s' module!",
                    currentModule.lessons().size(), currentProgress.currentModuleKey());
        }
        return String.format("Currently on lesson %d of %d in the '%s' module.",
                currentProgress.currentLessonIndex() + 1, currentModule.lessons().size(), currentProgress.currentModuleKey());
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
        if (!isModuleComplete()) {
            throw new IllegalStateException("You must finish the current set of lessons before generating more.");
        }
        if (currentModule == null) {
            throw new IllegalStateException("There is no active module.");
        }

        // Generate the new lessons, telling the AI what we've already learned
        GitModule newLessonsModule = aiTutorService.generateMoreLessons(
                currentProgress.currentModuleKey(),
                currentModule.lessons()
        );

        // Combine the old and new lessons
        List<Lesson> combinedLessons = new ArrayList<>(currentModule.lessons());
        combinedLessons.addAll(newLessonsModule.lessons());

        // Create a new module object with the combined list
        this.currentModule = new GitModule(currentModule.moduleName(), combinedLessons);

        System.out.println("Successfully added " + newLessonsModule.lessons().size() + " new lessons to the module.");
        // The user's progress index is already at the correct position to start the new lessons on the next 'next' command
        saveProgress();
    }


    private void saveProgress() {
        try {
            objectMapper.writeValue(PROGRESS_FILE.toFile(), currentProgress);
        } catch (IOException e) {
            System.err.println("Warning: Could not save progress.");
        }
    }
}