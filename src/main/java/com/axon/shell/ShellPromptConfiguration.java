package com.axon.shell;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.jline.PromptProvider;

@Configuration
public class ShellPromptConfiguration {
    @Bean
    public PromptProvider axonPrompt() {
        return () -> new AttributedString(
                "axon > ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT | AttributedStyle.CYAN)
        );
    }
}