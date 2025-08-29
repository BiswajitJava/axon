package com.axon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Lesson(
        String title,
        String concept,
        String command,
        String example_output,
        String practiceCommand,
        String hint
) {
}