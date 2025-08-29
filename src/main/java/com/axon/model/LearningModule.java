package com.axon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LearningModule(String moduleName, List<Lesson> lessons) {}