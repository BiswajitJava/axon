package com.axon.service.impl;

import com.axon.model.Lesson;
import com.axon.service.api.PromptService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("gitPromptService")
public class GitPromptServiceImpl implements PromptService {

    private final Map<String, String> modules = Map.of(
            "basics", "the absolute basics of Git, covering init, add, commit, status, and log",
            "branching", "Git branching, covering create, switch, merge, and delete branches",
            "remotes", "working with remote Git repositories, covering clone, push, pull, and fetch",
            "history", "inspecting and rewriting Git history, covering rebase, amend, and reset"
    );

    @Override
    public String getTechnologyName() {
        return "Git";
    }

    @Override
    public Map<String, String> getAvailableModules() {
        return Map.of(
                "basics", "Git Basics: The First Steps",
                "branching", "Mastering Git Branching",
                "remotes", "Working with Remote Repositories",
                "history", "Inspecting and Rewriting History"
        );
    }

    @Override
    public String buildInitialModulePrompt(String moduleKey) {
        String topic = modules.get(moduleKey);
        String prompt = """
        You are a curriculum generation bot. Your only function is to output a single, valid JSON object.
        Generate a curriculum for a developer learning about '%s'.
        The "lessons" array must contain exactly 30 lesson objects.
        Each lesson object MUST contain "title", "concept", "command", and "example_output".
        Inside "example_output", you MUST use these XML tags for colorization:
        - Branch names: <branch>...</branch>
        - Filenames/paths: <file>...</file>
        - Commit hashes: <commit>...</commit>
        Output only the raw JSON.
        """;
        return formatPrompt(String.format(prompt, topic));
    }

    @Override
    public String buildMoreLessonsPrompt(String moduleKey, List<Lesson> existingLessons) {
        String topic = modules.get(moduleKey);
        String completedCommands = existingLessons.stream()
                .map(Lesson::command)
                .map(cmd -> "`" + cmd + "`")
                .collect(Collectors.joining(", "));

        String prompt = """
        You are a curriculum generation bot outputting a single, valid JSON object.
        Generate a new curriculum with 15 more lessons for a developer learning about '%s'.
        CRITICAL: The user has already learned these commands: %s. You MUST NOT create lessons for these commands.
        Introduce NEW, more advanced, or related commands and concepts.
        The "lessons" array must contain exactly 15 lesson objects.
        Each lesson object must contain "title", "concept", "command", and "example_output" with the required <branch>, <file>, <commit> tags.
        Output only the raw JSON.
        """;
        return formatPrompt(String.format(prompt, topic, completedCommands));
    }

    @Override
    public String buildQuestionPrompt(String question) {
        String prompt = """
        You are an expert Git tutor. Provide a clear, concise explanation for the following user question.
        Use markdown for code blocks and emphasis.
        Question: "%s"
        """;
        return formatPrompt(String.format(prompt, question), false);
    }

    private String formatPrompt(String userContent) {
        return formatPrompt(userContent, true);
    }

    private String formatPrompt(String userContent, boolean isJsonOutput) {
        String systemMessage = isJsonOutput
                ? "You are a helpful assistant that only outputs valid JSON."
                : "You are a helpful assistant.";
        return String.format("<|im_start|>system\n%s<|im_end|>\n<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n",
                systemMessage, userContent);
    }
}