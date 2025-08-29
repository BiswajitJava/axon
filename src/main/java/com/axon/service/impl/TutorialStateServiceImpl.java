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
import java.util.Optional;

@Service
public class TutorialStateServiceImpl implements TutorialStateService {
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
        return String.format("Currently on lesson %d of the '%s' module.",
                currentProgress.currentLessonIndex() + 1, currentProgress.currentModuleKey());
    }

    private void saveProgress() {
        try {
            objectMapper.writeValue(PROGRESS_FILE.toFile(), currentProgress);
        } catch (IOException e) {
            System.err.println("Warning: Could not save progress.");
        }
    }
}