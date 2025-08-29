//package com.axon.shell;
//
//import com.axon.service.api.AiTutorService;
//import org.jline.terminal.Terminal;
//import org.jline.utils.AttributedString;
//import org.jline.utils.AttributedStringBuilder;
//import org.jline.utils.AttributedStyle;
//import org.springframework.shell.standard.ShellComponent;
//import org.springframework.shell.standard.ShellMethod;
//import org.springframework.shell.standard.ShellOption;
//
//@ShellComponent
//public class AskCommand {
//
//    private final AiTutorService aiTutorService;
//    private final Terminal terminal;
//
//    // Define some styles for the output
//    private static final AttributedStyle INFO_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).italic();
//    private static final AttributedStyle ERROR_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.RED);
//    private static final AttributedStyle AI_RESPONSE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
//    private static final AttributedStyle HEADER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
//
//
//    public AskCommand(AiTutorService aiTutorService, Terminal terminal) {
//        this.aiTutorService = aiTutorService;
//        this.terminal = terminal;
//    }
//
//    @ShellMethod(key = "ask", value = "Ask the AI for help with a Git command or concept.")
//    public void ask(@ShellOption(arity = Integer.MAX_VALUE, help = "Your question, e.g., 'what is the difference between git fetch and git pull?'") String[] questionParts) {
//        if (questionParts == null || questionParts.length == 0) {
//            terminal.writer().println(new AttributedString("Please provide a question after the 'ask' command.", ERROR_STYLE).toAnsi());
//            terminal.writer().flush();
//            return;
//        }
//
//        String question = String.join(" ", questionParts);
//
//        try {
//            terminal.writer().println(new AttributedString("Asking the AI tutor for help... please wait.", INFO_STYLE).toAnsi());
//            terminal.writer().flush();
//
//            String answer = aiTutorService.answerQuestion(question);
//
//            String separator = "─".repeat(terminal.getWidth());
//
//            String formattedAnswer = new AttributedStringBuilder()
//                    .append("\n")
//                    .style(HEADER_STYLE)
//                    .append(separator).append("\n")
//                    .append("AI TUTOR'S RESPONSE:\n")
//                    .append(separator).append("\n\n")
//                    .style(AI_RESPONSE_STYLE)
//                    .append(answer)
//                    .append("\n")
//                    .style(HEADER_STYLE)
//                    .append(separator).append("\n")
//                    .toAnsi();
//
//
//            terminal.writer().println(formattedAnswer);
//            terminal.writer().flush();
//
//        } catch (Exception e) {
//            String errorMessage = "Fatal Error: Could not get an answer from the AI. " + e.getMessage();
//            terminal.writer().println(new AttributedString(errorMessage, ERROR_STYLE).toAnsi());
//            terminal.writer().flush();
//        }
//    }
//}