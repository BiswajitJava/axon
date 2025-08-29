package com.axon.shell;

import com.axon.service.api.TutorialStateService;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class MoreCommand {

    private final TutorialStateService stateService;
    private final Terminal terminal;

    private static final AttributedStyle INFO_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).italic();
    private static final AttributedStyle SUCCESS_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.GREEN).bold();
    private static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.RED);

    public MoreCommand(TutorialStateService stateService, Terminal terminal) {
        this.stateService = stateService;
        this.terminal = terminal;
    }

    @ShellMethod(key = "more", value = "Generate more lessons for the current topic after completing a module.")
    public void more() {
        if (!stateService.isModuleComplete()) {
            terminal.writer().println(new AttributedString("You can only request more lessons after finishing all lessons in the current module.", ERROR_STYLE).toAnsi());
            terminal.writer().flush();
            return;
        }

        try {
            terminal.writer().println(new AttributedString("Generating more advanced lessons... this may take a moment.", INFO_STYLE).toAnsi());
            terminal.writer().flush();

            stateService.appendMoreLessons();

            String successMessage = new AttributedStringBuilder()
                    .append("\nNew lessons have been added to your learning path!\n", SUCCESS_STYLE)
                    .append("Type 'next' to continue your journey.", INFO_STYLE)
                    .toAnsi();

            terminal.writer().println(successMessage);
            terminal.writer().flush();

        } catch (Exception e) {
            String errorMessage = "Error: Could not generate more lessons. " + e.getMessage();
            terminal.writer().println(new AttributedString(errorMessage, ERROR_STYLE).toAnsi());
            terminal.writer().flush();
        }
    }
}