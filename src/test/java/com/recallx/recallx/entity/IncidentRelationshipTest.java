package com.recallx.recallx.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class IncidentRelationshipTest {

    @Test
    public void testIncidentTroubleshootingRelationship() {
        Incident incident = Incident.builder()
                .title("Database Timeout")
                .description("Service stopped responding")
                .build();

        TroubleshootingAttempt attempt1 = TroubleshootingAttempt.builder()
                .actionTaken("Restart service")
                .status(TroubleshootingStatus.FAILED)
                .incident(incident)
                .build();
                
        TroubleshootingAttempt attempt2 = TroubleshootingAttempt.builder()
                .actionTaken("Increase connection pool")
                .status(TroubleshootingStatus.SUCCEEDED)
                .incident(incident)
                .build();

        incident.setTroubleshootingAttempts(List.of(attempt1, attempt2));

        assertEquals(2, incident.getTroubleshootingAttempts().size());
        assertEquals(TroubleshootingStatus.FAILED, incident.getTroubleshootingAttempts().get(0).getStatus());
        assertEquals("Database Timeout", attempt2.getIncident().getTitle());
    }
}
