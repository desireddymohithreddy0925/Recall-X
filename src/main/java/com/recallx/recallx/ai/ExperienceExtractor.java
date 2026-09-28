package com.recallx.recallx.ai;

import com.recallx.recallx.dto.response.ExtractedExperience;

public interface ExperienceExtractor {
    ExtractedExperience extractFromRawInput(String rawInput);
}
