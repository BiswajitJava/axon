package com.axon.service.impl;

import com.axon.model.Lesson;
import com.axon.service.api.PromptService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("linuxPromptService")
public class LinuxPromptServiceImpl implements PromptService {

    private final Map<String, String> modules = Map.of(
            "files", "basic file system navigation and manipulation, covering ls, cd, pwd, cp, mv, rm, and mkdir",
            "permissions", "managing file permissions and ownership, covering chmod, chown, and the meaning of rwx",
            "processes", "managing system processes, covering ps, top, kill, and nice",
            "text", "processing text and using pipes, covering cat, grep, wc, head, tail, and the | operator"
    );

    @Override
    public String getTechnologyName() {
        return "Linux";
    }

    @Override
    public Map<String, String> getAvailableModules() {
        return Map.of(
                "files", "Linux Files & Directories",
                "permissions", "Understanding Permissions",
                "processes", "Process Management",
                "text", "Text Processing & Pipes"
        );
    }

    @Override
    public String buildInitialModulePrompt(String moduleKey) {
        String topic = modules.get(moduleKey);
        String prompt = """
        You are a curriculum generation bot. Your only function is to output a single, valid JSON object.
        Generate a curriculum for a developer learning about '%s'.
        The "lessons" array must contain exactly 20 lesson objects.
        Each lesson object MUST contain "title", "concept", "command", and "example_output".
        Inside "example_output", you MUST use these XML tags for colorization:
        - Filenames and directory paths: <path>...</path>
        - User or group names: <user>...</user>
        - Process IDs (PIDs): <pid>...</pid>
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
        Generate a new curriculum with 10 more lessons for a developer learning about '%s'.
        CRITICAL: The user has already learned these commands: %s. You MUST NOT create lessons for these commands.
        Introduce NEW, more advanced, or related commands and concepts.
        The "lessons" array must contain exactly 10 lesson objects.
        Each lesson object must contain "title", "concept", "command", and "example_output" with the required <path>, <user>, <pid> tags.
        Output only the raw JSON.
        """;
        return formatPrompt(String.format(prompt, topic, completedCommands));
    }

    @Override
    public String buildQuestionPrompt(String question) {
        String prompt = """
        You are an expert Linux System Administrator tutor. Provide a clear, concise explanation for the following user question.
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