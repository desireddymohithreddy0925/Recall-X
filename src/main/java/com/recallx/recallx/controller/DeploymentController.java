package com.recallx.recallx.controller;

import com.recallx.recallx.service.DeploymentAnalysisService;
import com.recallx.recallx.dto.response.RiskAnalysisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentAnalysisService deploymentAnalysisService;

    public DeploymentController(DeploymentAnalysisService deploymentAnalysisService) {
        this.deploymentAnalysisService = deploymentAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<RiskAnalysisResponse> submitAndAnalyzeDeployment(@RequestBody String deploymentInfo) {
        // In real app, extract organizationId from SecurityContext
        Long mockOrgId = 1L;
        RiskAnalysisResponse response = deploymentAnalysisService.analyzeDeployment(deploymentInfo, mockOrgId);
        
        // Persist the analysis for auditability here (omitted in stub)
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/analysis")
    public ResponseEntity<?> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/evidence")
    public ResponseEntity<?> getEvidence(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
}
