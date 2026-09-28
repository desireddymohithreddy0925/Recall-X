package com.recallx.recallx.agent;

import com.recallx.recallx.dto.response.ReasonedInsight;
import com.recallx.recallx.memory.dto.StructuredExperience;
import java.util.List;

public interface MemoryReasoningAgent {
    ReasonedInsight reasonOverMemories(String currentContext, List<StructuredExperience> retrievedMemories, Long organizationId);
}
