package com.axon.shell;

import com.axon.model.Lesson;
import com.axon.service.api.PromptService;
import com.axon.service.api.TutorialStateService;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ShellComponent
public class TutorCommands {

    private final TutorialStateService stateService;
    private final Terminal terminal;
    private final Map<String, PromptService> promptServiceMap;

    // --- UI STYLES ---
    private static final AttributedStyle HEADER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
    private static final AttributedStyle LABEL_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.CYAN);
    private static final AttributedStyle KEY_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.YELLOW);
    private static final AttributedStyle INFO_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).italic();
    private static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.RED);
    private static final AttributedStyle SUCCESS_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.GREEN).bold();
    private static final AttributedStyle LESSON_TITLE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
    private static final AttributedStyle COMMAND_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.GREEN);
    private static final AttributedStyle CONCEPT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).italic();
    private static final AttributedStyle OUTPUT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

    // Technology-specific styles
    private static final AttributedStyle GIT_BRANCH_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
    private static final AttributedStyle GIT_FILE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.CYAN);
    private static final AttributedStyle GIT_COMMIT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle DOCKER_IMAGE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.CYAN);
    private static final AttributedStyle DOCKER_CONTAINER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle DOCKER_VOLUME_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);

    private String lastUsedTechnology = null;
    private boolean inPracticeMode = false;
    private Lesson currentPracticeLesson = null;

    public TutorCommands(TutorialStateService stateService, Terminal terminal, List<PromptService> promptServices) {
        this.stateService = stateService;
        this.terminal = terminal;
        this.promptServiceMap = promptServices.stream()
                .collect(Collectors.toMap(s -> s.getTechnologyName().toLowerCase(), Function.identity()));
    }

    // --- Core Commands (list, start, etc.) ---
    // These methods remain unchanged from the previous correct version.
    // Full code included for completeness.

    @ShellMethod(key = "list", value = "List all available technologies and their learning modules.")
    public void list() {
        terminal.writer().println(new AttributedString("\nAvailable Learning Tracks:", HEADER_STYLE).toAnsi());
        terminal.writer().println("─".repeat(40));

        promptServiceMap.values().forEach(service -> {
            String techName = service.getTechnologyName();
            terminal.writer().println(new AttributedString("\n" + techName + " Modules:", LABEL_STYLE).toAnsi());

            service.getAvailableModules().forEach((key, name) -> {
                String formattedLine = new AttributedStringBuilder()
                        .append("  - ")
                        .style(KEY_STYLE).append(String.format("%-12s", key))
                        .style(AttributedStyle.DEFAULT).append(" | ").append(name)
                        .toAnsi();
                terminal.writer().println(formattedLine);
            });
        });

        terminal.writer().println(new AttributedString("\nType 'start [technology] [module_key]' to begin (e.g., 'start git basics').", INFO_STYLE).toAnsi());
        terminal.writer().flush();
    }

    @ShellMethod(key = "start", value = "Start a new learning module for a specific technology.")
    public void start(
            @ShellOption(help = "The technology to learn (e.g., 'git', 'docker').") String technology,
            @ShellOption(help = "The key of the module to start (e.g., 'basics').") String moduleKey
    ) {
        String techKey = technology.toLowerCase();
        if (!promptServiceMap.containsKey(techKey)) {
            terminal.writer().println(new AttributedString("Error: Unknown technology '" + technology + "'.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }

        try {
            terminal.writer().println(new AttributedString("Please wait, generating your personalized lesson plan from the AI...", INFO_STYLE).toAnsi());
            stateService.startModule(techKey, moduleKey);
            this.lastUsedTechnology = techKey;

            this.inPracticeMode = false;
            this.currentPracticeLesson = null;

            displayCurrentLesson();
        } catch (Exception e) {
            String errorMessage = "Fatal Error: " + e.getMessage();
            terminal.writer().println(new AttributedString(errorMessage, ERROR_STYLE).toAnsi());
            terminal.writer().flush();
        }
    }

    // --- Navigation and Other Commands ---
    // These methods are also unchanged.

    @ShellMethod(key = "next", value = "Proceed to the next lesson in the current module.")
    public void next() {
        if (inPracticeMode) {
            terminal.writer().println(new AttributedString("You must complete the practice exercise first. Use 'practice [command]', 'hint', or 'skip'.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        stateService.getNextLesson();
        displayCurrentLesson();
    }

    @ShellMethod(key = "prev", value = "Return to the previous lesson.")
    public void prev() {
        if (inPracticeMode) {
            terminal.writer().println(new AttributedString("You cannot go back during a practice exercise. Please 'skip' or complete it first.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        Optional<Lesson> lessonOpt = stateService.getPreviousLesson();
        if (lessonOpt.isPresent()) {
            displayCurrentLesson();
        } else {
            terminal.writer().println(new AttributedString("You are already on the first lesson.", INFO_STYLE).toAnsi());
            terminal.writer().flush();
        }
    }

    @ShellMethod(key = "toc", value = "Show the table of contents for the current module.")
    public void toc() {
        List<Lesson> lessons = stateService.getCurrentModuleLessons();
        if (lessons.isEmpty()) {
            terminal.writer().println(new AttributedString("No active module. Use 'start' to begin.", INFO_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }

        terminal.writer().println(new AttributedString("\nTable of Contents:", HEADER_STYLE).toAnsi());
        terminal.writer().println("─".repeat(40));
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            terminal.writer().println(String.format("[%d] %s", i + 1, lesson.title()));
        }
        terminal.writer().println("─".repeat(40));
        terminal.writer().flush();
    }

    @ShellMethod(key = "goto", value = "Jump to a specific lesson number.")
    public void goTo(@ShellOption(help = "The lesson number from the 'toc'.") int lessonNumber) {
        if (inPracticeMode) {
            terminal.writer().println(new AttributedString("You cannot jump to another lesson during a practice exercise.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        Optional<Lesson> lessonOpt = stateService.goToLesson(lessonNumber);
        if (lessonOpt.isEmpty()) {
            terminal.writer().println(new AttributedString("Error: Invalid lesson number. Use 'toc' to see the list.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
        } else {
            displayCurrentLesson();
        }
    }

    // ... other commands like more, ask, status, summary remain the same ...
    @ShellMethod(key = "more", value = "Generate more lessons for the current topic after completing a module.")
    public void more() {
        if (!stateService.isModuleComplete()) {
            terminal.writer().println(new AttributedString("You must finish the current set of lessons before requesting more.", ERROR_STYLE).toAnsi());
            return;
        }
        try {
            terminal.writer().println(new AttributedString("Generating more advanced lessons... this may take a moment.", INFO_STYLE).toAnsi());
            stateService.appendMoreLessons();
            terminal.writer().println(new AttributedString("\nNew lessons have been added! Type 'next' to continue.", SUCCESS_STYLE).toAnsi());
        } catch (Exception e) {
            terminal.writer().println(new AttributedString("Error: Could not generate more lessons. " + e.getMessage(), ERROR_STYLE).toAnsi());
        }
        terminal.writer().flush();
    }

    @ShellMethod(key = "ask", value = "Ask the AI for help about the current technology.")
    public void ask(@ShellOption(arity = Integer.MAX_VALUE, help = "Your question.") String[] questionParts) {
        if (questionParts == null || questionParts.length == 0) {
            terminal.writer().println(new AttributedString("Please provide a question after the 'ask' command.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        try {
            String question = String.join(" ", questionParts);
            terminal.writer().println(new AttributedString("Asking the AI tutor for help...", INFO_STYLE).toAnsi());
            String answer = stateService.answerQuestion(question);

            String separator = "─".repeat(terminal.getWidth());
            terminal.writer().println("\n" + separator);
            terminal.writer().println(new AttributedString("AI TUTOR'S RESPONSE:", HEADER_STYLE).toAnsi());
            terminal.writer().println(separator + "\n");
            terminal.writer().println(new AttributedString(answer, CONCEPT_STYLE).toAnsi());
            terminal.writer().println("\n" + separator);
        } catch (Exception e) {
            terminal.writer().println(new AttributedString("Fatal Error: Could not get an answer from the AI. " + e.getMessage(), ERROR_STYLE).toAnsi());
        }
        terminal.writer().flush();
    }

    @ShellMethod(key = "status", value = "Check your current tutorial progress.")
    public void status() {
        terminal.writer().println(new AttributedString(stateService.getStatus(), LABEL_STYLE).toAnsi());
        terminal.writer().flush();
    }

    @ShellMethod(key = "summary", value = "Generate an AI summary of the completed module.")
    public void summary() {
        try {
            String summaryText = stateService.generateSummary();
            String separator = "─".repeat(terminal.getWidth());
            terminal.writer().println("\n" + separator);
            terminal.writer().println(new AttributedString("AI-POWERED MODULE SUMMARY:", HEADER_STYLE).toAnsi());
            terminal.writer().println(separator + "\n");
            terminal.writer().println(new AttributedString(summaryText, CONCEPT_STYLE).toAnsi());
            terminal.writer().println("\n" + separator);
        } catch (Exception e) {
            terminal.writer().println(new AttributedString("Error: " + e.getMessage(), ERROR_STYLE).toAnsi());
        }
        terminal.writer().flush();
    }


    // --- Practice Mode Commands ---
    // Unchanged.

    @ShellMethod(key = "practice", value = "Submit your answer for the current practice exercise.")
    public void practice(@ShellOption(arity = Integer.MAX_VALUE, help = "The full command you want to practice.") String[] commandParts) {
        if (!inPracticeMode || currentPracticeLesson == null) {
            terminal.writer().println(new AttributedString("There is no active practice exercise.", INFO_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }

        String userInput = String.join(" ", commandParts);
        if (userInput.equals(currentPracticeLesson.practiceCommand())) {
            terminal.writer().println(new AttributedString("\nCorrect! Well done.", SUCCESS_STYLE).toAnsi());
            this.inPracticeMode = false;
            this.currentPracticeLesson = null;
            next();
        } else {
            terminal.writer().println(new AttributedString("Not quite. Please try again. Type 'hint' if you're stuck.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
        }
    }

    @ShellMethod(key = "hint", value = "Get a hint for the current practice exercise.")
    public void hint() {
        if (!inPracticeMode || currentPracticeLesson == null) {
            terminal.writer().println(new AttributedString("There is no active practice exercise.", INFO_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        String hintText = currentPracticeLesson.hint();
        if (hintText == null || hintText.isBlank()) {
            terminal.writer().println(new AttributedString("Sorry, no hint is available for this lesson.", INFO_STYLE).toAnsi());
        } else {
            terminal.writer().println(new AttributedString("Hint: " + hintText, INFO_STYLE).toAnsi());
        }
        terminal.writer().flush();
    }

    @ShellMethod(key = "skip", value = "Skip the current practice exercise and move to the next lesson.")
    public void skip() {
        if (!inPracticeMode) {
            terminal.writer().println(new AttributedString("There is no active practice exercise to skip.", INFO_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }
        terminal.writer().println(new AttributedString("Skipping exercise...", INFO_STYLE).toAnsi());
        this.inPracticeMode = false;
        this.currentPracticeLesson = null;
        next();
    }

    // --- Core Display Logic (THIS IS WHERE THE FIX IS) ---

    private void displayCurrentLesson() {
        Optional<Lesson> lessonOpt = stateService.getCurrentLesson();
        if (lessonOpt.isEmpty()) {
            // --- Module is complete ---
            this.inPracticeMode = false;
            this.currentPracticeLesson = null;
            String completionMessage = new AttributedStringBuilder()
                    .append("\nCongratulations, you have completed the module!\n", SUCCESS_STYLE)
                    .append("Type 'more' to generate more lessons or 'summary' for a review.", INFO_STYLE)
                    .toAnsi();
            terminal.writer().println(completionMessage);
        } else {
            // --- A lesson is available ---
            Lesson lesson = lessonOpt.get();

            // Step 1: ALWAYS print the full lesson content first.
            String formattedLesson = formatLessonForDisplay(lesson);
            terminal.writer().println(formattedLesson);

            // Step 2: THEN, determine the next action and prompt the user.
            if (lesson.practiceCommand() != null && !lesson.practiceCommand().isBlank()) {
                // This lesson has a practice component.
                this.inPracticeMode = true;
                this.currentPracticeLesson = lesson;
                terminal.writer().println(new AttributedStringBuilder()
                        .append("\n▶️ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append("PRACTICE: Use the 'practice' command to submit your answer.", INFO_STYLE)
                        .toAnsi()
                );
            } else {
                // This is a conceptual lesson with nothing to practice.
                this.inPracticeMode = false;
                this.currentPracticeLesson = null;
                terminal.writer().println(new AttributedStringBuilder()
                        .append("\n", INFO_STYLE) // Add a newline for spacing
                        .append("Type 'next' to continue or 'ask [question]' for help.")
                        .toAnsi()
                );
            }
        }
        terminal.writer().flush();
    }

    private String formatLessonForDisplay(Lesson lesson) {
        String separator = "─".repeat(terminal.getWidth());

        AttributedStringBuilder builder = new AttributedStringBuilder()
                .append("\n").style(HEADER_STYLE).append(separator).append("\n")
                .style(LESSON_TITLE_STYLE).append("Lesson: ").append(lesson.title()).append("\n")
                .style(HEADER_STYLE).append(separator).append("\n\n")
                .style(LABEL_STYLE).append("[CONCEPT]: ").style(CONCEPT_STYLE).append(lesson.concept()).append("\n\n");

        if (lesson.command() != null && !lesson.command().isBlank()) {
            builder.style(LABEL_STYLE).append("[COMMAND]:\n")
                    .style(COMMAND_STYLE).append("  ").append(lesson.command()).append("\n\n");
        }

        if (lesson.example_output() != null && !lesson.example_output().isBlank()) {
            AttributedString colorizedOutput = parseAndColorize(lesson.example_output());
            builder.style(LABEL_STYLE).append("[EXAMPLE OUTPUT]:\n")
                    .append(colorizedOutput).append("\n"); // Removed extra newline for tighter spacing
        }

        builder.style(HEADER_STYLE).append(separator);

        return builder.toAnsi();
    }

    // The parseAndColorize and addTechnologySpecificColoring methods are unchanged
    // but included here for completeness.
    private AttributedString parseAndColorize(String text) {
        if (lastUsedTechnology == null) {
            String techGuess = stateService.getStatus().contains("Docker") ? "docker" : "git";
            lastUsedTechnology = techGuess;
        }

        Pattern pattern;
        if (lastUsedTechnology.equals("git")) {
            pattern = Pattern.compile("<(branch|file|commit)>(.*?)</\\1>");
        } else if (lastUsedTechnology.equals("docker")) {
            pattern = Pattern.compile("<(image|container|volume)>(.*?)</\\1>");
        } else if (lastUsedTechnology.equals("linux")) {
            pattern = Pattern.compile("<(path|user|pid)>(.*?)</\\1>");
        } else if (lastUsedTechnology.equals("kubernetes")) {
            pattern = Pattern.compile("<(resource|type|namespace)>(.*?)</\\1>");
        }
        else {
            return new AttributedString(text, OUTPUT_STYLE);
        }

        AttributedStringBuilder builder = new AttributedStringBuilder();
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            builder.style(OUTPUT_STYLE).append(text.substring(lastEnd, matcher.start()));
            String tagType = matcher.group(1);
            String content = matcher.group(2);
            addTechnologySpecificColoring(builder, tagType, content);
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            builder.style(OUTPUT_STYLE).append(text.substring(lastEnd));
        }
        return builder.toAttributedString();
    }

    private void addTechnologySpecificColoring(AttributedStringBuilder builder, String tagType, String content) {
        switch (lastUsedTechnology) {
            case "git" -> {
                switch (tagType) {
                    case "branch" -> builder.style(GIT_BRANCH_STYLE).append(content);
                    case "file" -> builder.style(GIT_FILE_STYLE).append(content);
                    case "commit" -> builder.style(GIT_COMMIT_STYLE).append(content);
                    default -> builder.style(OUTPUT_STYLE).append(content);
                }
            }
            case "docker" -> {
                switch (tagType) {
                    case "image" -> builder.style(DOCKER_IMAGE_STYLE).append(content);
                    case "container" -> builder.style(DOCKER_CONTAINER_STYLE).append(content);
                    case "volume" -> builder.style(DOCKER_VOLUME_STYLE).append(content);
                    default -> builder.style(OUTPUT_STYLE).append(content);
                }
            }
            case "linux" -> {
                switch (tagType) {
                    case "path" -> builder.style(GIT_FILE_STYLE).append(content);
                    case "user" -> builder.style(DOCKER_CONTAINER_STYLE).append(content);
                    case "pid" -> builder.style(GIT_COMMIT_STYLE).append(content);
                    default -> builder.style(OUTPUT_STYLE).append(content);
                }
            }
            case "kubernetes" -> {
                switch (tagType) {
                    case "resource" -> builder.style(DOCKER_CONTAINER_STYLE).append(content);
                    case "type" -> builder.style(KEY_STYLE).append(content);
                    case "namespace" -> builder.style(GIT_BRANCH_STYLE).append(content);
                    default -> builder.style(OUTPUT_STYLE).append(content);
                }
            }
        }
    }
}