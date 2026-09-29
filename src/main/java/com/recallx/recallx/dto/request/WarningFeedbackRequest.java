package com.recallx.recallx.dto.request;

import lombok.Data;

@Data
public class WarningFeedbackRequest {
    private String warningId;
    private String outcome; // USEFUL, FALSE_POSITIVE, IGNORED
    private String reason; // Required if FALSE_POSITIVE
}
