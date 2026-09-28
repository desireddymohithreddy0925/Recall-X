package com.recallx.recallx.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PatternDTO {
    private Long id;
    private String patternName;
    private String description;
    private String triggerEvent;
    private String cascadingEffect;
    private String resultingIncident;
    private int occurrenceCount;
    private List<String> evidenceReferences;
    private List<String> relatedLessons;
}
