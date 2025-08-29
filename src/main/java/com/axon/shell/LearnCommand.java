//package com.axon.shell;
//
//import com.axon.model.Lesson;
//import com.axon.service.api.AiTutorService;
//import com.axon.service.api.TutorialStateService;
//import org.jline.terminal.Terminal;
//import org.jline.utils.AttributedString;
//import org.jline.utils.AttributedStringBuilder;
//import org.jline.utils.AttributedStyle;
//import org.springframework.shell.standard.ShellComponent;
//import org.springframework.shell.standard.ShellMethod;
//import org.springframework.shell.standard.ShellOption;
//import java.util.Optional;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@ShellComponent("learn")
//public class LearnCommand {
//
//    // --- START: THE FINAL, CORRECTED COLOR PALETTE ---
//    private static final AttributedStyle LESSON_TITLE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
//    // CORRECTED: Labels are now Orange (rendered as Yellow in most terminals)
//    private static final AttributedStyle LABEL_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
//    private static final AttributedStyle COMMAND_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.GREEN);
//
//    // Other UI colors
//    private static final AttributedStyle HEADER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
//    private static final AttributedStyle KEY_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.YELLOW);
//    private static final AttributedStyle CONCEPT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).italic();
//    // CORRECTED: Default example output color is now Blue
//    private static final AttributedStyle OUTPUT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
//    private static final AttributedStyle INFO_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).italic();
//    private static final AttributedStyle SUCCESS_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.GREEN).bold();
//    private static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.RED);
//
//    // Colors for AI-generated tagged content (these will override the blue for specific parts)
//    private static final AttributedStyle BRANCH_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
//    private static final AttributedStyle FILE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.CYAN);
//    private static final AttributedStyle COMMIT_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
//    // --- END: THE FINAL, CORRECTED COLOR PALETTE ---
//
//    private final AiTutorService aiTutorService;
//    private final TutorialStateService stateService;
//    private final Terminal terminal;
//
//    public LearnCommand(AiTutorService aiTutorService, TutorialStateService stateService, Terminal terminal) {
//        this.aiTutorService = aiTutorService;
//        this.stateService = stateService;
//        this.terminal = terminal;
//    }
//
//    @ShellMethod(key = "list", value = "List all available learning modules.")
//    public void list() {
//        terminal.writer().println("\nAvailable Learning Modules:");
//        aiTutorService.getAvailableModules().forEach((key, name) -> {
//            String formattedLine = new AttributedStringBuilder()
//                    .append("  - ")
//                    .style(KEY_STYLE)
//                    .append(String.format("%-12s", key))
//                    .style(AttributedStyle.DEFAULT)
//                    .append(" | ")
//                    .append(name)
//                    .toAnsi();
//            terminal.writer().println(formattedLine);
//        });
//        terminal.writer().println(new AttributedString("\nType 'start [key]' to begin a module.", INFO_STYLE).toAnsi());
//        terminal.writer().flush();
//    }
//
//    @ShellMethod(key = "start", value = "Start a new learning module.")
//    public void start(@ShellOption(help = "The key of the module to start, e.g., 'basics'.") String moduleKey) {
//        if (!aiTutorService.getAvailableModules().containsKey(moduleKey)) {
//            terminal.writer().println(new AttributedString("Error: Unknown module key.", ERROR_STYLE).toAnsi());
//            terminal.writer().flush();
//            return;
//        }
//
//        try {
//            terminal.writer().println(new AttributedString("Please wait, generating your personalized lesson plan from the AI...", INFO_STYLE).toAnsi());
//            terminal.writer().flush();
//            stateService.startModule(moduleKey);
//            displayCurrentLesson();
//        } catch (Exception e) {
//            String errorMessage = "Fatal Error: " + e.getMessage();
//            terminal.writer().println(new AttributedString(errorMessage, ERROR_STYLE).toAnsi());
//            terminal.writer().flush();
//        }
//    }
//
//    @ShellMethod(key = "next", value = "Proceed to the next lesson in the current module.")
//    public void next() {
//        stateService.getNextLesson();
//        displayCurrentLesson();
//    }
//
//    @ShellMethod(key = "status", value = "Check your current tutorial progress.")
//    public AttributedString status() {
//        String statusText = stateService.getStatus();
//        return new AttributedString(statusText, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
//    }
//
//    private void displayCurrentLesson() {
//        Optional<Lesson> lessonOpt = stateService.getCurrentLesson();
//        if (lessonOpt.isEmpty()) {
//            String completionMessage = new AttributedStringBuilder()
//                    .append("\nCongratulations, you have completed the module!\n", SUCCESS_STYLE)
//                    .append("Type 'more' to generate more lessons for this topic.\n", INFO_STYLE)
//                    .append("Or type 'list' to see what you can learn next.", INFO_STYLE)
//                    .toAnsi();
//            terminal.writer().println(completionMessage);
//        } else {
//            String formattedLesson = formatLessonForDisplay(lessonOpt.get());
//            terminal.writer().println(formattedLesson);
//        }
//        terminal.writer().flush();
//    }
//
//    private String formatLessonForDisplay(Lesson lesson) {
//        String separator = "─".repeat(terminal.getWidth());
//        AttributedString colorizedOutput = parseAndColorize(lesson.example_output());
//
//        return new AttributedStringBuilder()
//                .append("\n")
//                .style(HEADER_STYLE)
//                .append(separator).append("\n")
//                .style(LESSON_TITLE_STYLE)
//                .append("Lesson: ").append(lesson.title()).append("\n")
//                .style(HEADER_STYLE)
//                .append(separator).append("\n\n")
//
//                .style(LABEL_STYLE)
//                .append("[CONCEPT]: ")
//                .style(CONCEPT_STYLE)
//                .append(lesson.concept()).append("\n\n")
//
//                .style(LABEL_STYLE)
//                .append("[COMMAND]:\n")
//                .style(COMMAND_STYLE)
//                .append("  ").append(lesson.command()).append("\n\n")
//
//                .style(LABEL_STYLE)
//                .append("[EXAMPLE OUTPUT]:\n")
//                .append(colorizedOutput)
//                .append("\n\n")
//
//                .style(HEADER_STYLE)
//                .append(separator).append("\n")
//                .style(INFO_STYLE)
//                .append("Type 'next' to continue... or 'start [key]' to begin a new module.\n")
//                .toAnsi();
//    }
//
//    private AttributedString parseAndColorize(String text) {
//        AttributedStringBuilder builder = new AttributedStringBuilder();
//        Pattern pattern = Pattern.compile("<(branch|file|commit)>(.*?)</\\1>");
//        Matcher matcher = pattern.matcher(text);
//        int lastEnd = 0;
//        while (matcher.find()) {
//            builder.style(OUTPUT_STYLE).append(text.substring(lastEnd, matcher.start()));
//            String tagType = matcher.group(1);
//            String content = matcher.group(2);
//            switch (tagType) {
//                case "branch" -> builder.style(BRANCH_STYLE).append(content);
//                case "file" -> builder.style(FILE_STYLE).append(content);
//                case "commit" -> builder.style(COMMIT_STYLE).append(content);
//                default -> builder.style(OUTPUT_STYLE).append(content);
//            }
//            lastEnd = matcher.end();
//        }
//        if (lastEnd < text.length()) {
//            builder.style(OUTPUT_STYLE).append(text.substring(lastEnd));
//        }
//        return builder.toAttributedString();
//    }
//}