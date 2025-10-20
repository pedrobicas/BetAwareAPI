package com.example.betaware.controller;

import com.example.betaware.service.LGPDComplianceService;
import com.example.betaware.service.VulnerabilityManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controlador do Dashboard de Segurança
 * Fornece endpoints para monitoramento e relatórios de segurança
 */
@RestController
@RequestMapping("/api/v1/security")
@PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
public class SecurityDashboardController {

    @Autowired
    private LGPDComplianceService lgpdService;

    @Autowired
    private VulnerabilityManagementService vulnerabilityService;

    /**
     * Dashboard Principal - Visão Geral de Segurança
     */
    @GetMapping("/dashboard")
    public ResponseEntity<SecurityDashboard> getSecurityDashboard() {
        SecurityDashboard dashboard = new SecurityDashboard();
        
        // Métricas LGPD
        LGPDComplianceService.ComplianceMetrics lgpdMetrics = lgpdService.getMetrics();
        dashboard.setLgpdMetrics(lgpdMetrics);
        
        // Métricas de Vulnerabilidades
        VulnerabilityManagementService.VulnerabilityMetrics vulnMetrics = vulnerabilityService.getVulnerabilityMetrics();
        dashboard.setVulnerabilityMetrics(vulnMetrics);
        
        // Status Geral de Segurança
        dashboard.setOverallSecurityStatus(calculateOverallSecurityStatus(lgpdMetrics, vulnMetrics));
        
        // Alertas Críticos
        dashboard.setCriticalAlerts(generateCriticalAlerts(lgpdMetrics, vulnMetrics));
        
        // Tendências de Segurança
        dashboard.setSecurityTrends(generateSecurityTrends());
        
        dashboard.setLastUpdated(LocalDateTime.now());
        
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Relatório de Conformidade LGPD
     */
    @GetMapping("/lgpd/compliance-report")
    public ResponseEntity<LGPDComplianceService.ComplianceReport> getLGPDComplianceReport() {
        LGPDComplianceService.ComplianceReport report = lgpdService.generateComplianceReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Métricas LGPD Detalhadas
     */
    @GetMapping("/lgpd/metrics")
    public ResponseEntity<LGPDComplianceService.ComplianceMetrics> getLGPDMetrics() {
        LGPDComplianceService.ComplianceMetrics metrics = lgpdService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Consentimentos por Usuário
     */
    @GetMapping("/lgpd/consents/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<List<LGPDComplianceService.ConsentRecord>> getUserConsents(@PathVariable String userId) {
        List<LGPDComplianceService.ConsentRecord> consents = lgpdService.getUserConsents(userId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Solicitações de Titulares de Dados
     */
    @GetMapping("/lgpd/data-subject-requests")
    public ResponseEntity<Map<String, Object>> getDataSubjectRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        // Simular paginação de solicitações
        Map<String, Object> response = new HashMap<>();
        response.put("requests", generateMockDataSubjectRequests(page, size, status));
        response.put("totalElements", 150);
        response.put("totalPages", 8);
        response.put("currentPage", page);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Relatório de Vulnerabilidades
     */
    @GetMapping("/vulnerabilities/report")
    public ResponseEntity<VulnerabilityManagementService.VulnerabilityReport> getVulnerabilityReport() {
        VulnerabilityManagementService.VulnerabilityReport report = vulnerabilityService.generateVulnerabilityReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Métricas de Vulnerabilidades
     */
    @GetMapping("/vulnerabilities/metrics")
    public ResponseEntity<VulnerabilityManagementService.VulnerabilityMetrics> getVulnerabilityMetrics() {
        VulnerabilityManagementService.VulnerabilityMetrics metrics = vulnerabilityService.getVulnerabilityMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Vulnerabilidades por Severidade
     */
    @GetMapping("/vulnerabilities/by-severity")
    public ResponseEntity<Map<String, Integer>> getVulnerabilitiesBySeverity() {
        VulnerabilityManagementService.VulnerabilityMetrics metrics = vulnerabilityService.getVulnerabilityMetrics();
        
        Map<String, Integer> bySeverity = new HashMap<>();
        bySeverity.put("CRITICAL", metrics.getCriticalCount());
        bySeverity.put("HIGH", metrics.getHighSeverityCount());
        bySeverity.put("MEDIUM", metrics.getMediumSeverityCount());
        bySeverity.put("LOW", metrics.getLowSeverityCount());
        
        return ResponseEntity.ok(bySeverity);
    }

    /**
     * Histórico de Scans de Segurança
     */
    @GetMapping("/scans/history")
    public ResponseEntity<List<SecurityScanResult>> getSecurityScansHistory(
            @RequestParam(defaultValue = "30") int days) {
        
        List<SecurityScanResult> scanHistory = generateMockScanHistory(days);
        return ResponseEntity.ok(scanHistory);
    }

    /**
     * Métricas de Performance de Segurança
     */
    @GetMapping("/performance/metrics")
    public ResponseEntity<SecurityPerformanceMetrics> getSecurityPerformanceMetrics() {
        SecurityPerformanceMetrics metrics = new SecurityPerformanceMetrics();
        
        // Simular métricas de performance
        metrics.setAverageAuthenticationTime(45.2);
        metrics.setAverageAuthorizationTime(12.8);
        metrics.setSecurityValidationTime(8.5);
        metrics.setTotalSecurityChecks(15420);
        metrics.setFailedSecurityChecks(23);
        metrics.setSecurityCheckSuccessRate(99.85);
        metrics.setLastCalculated(LocalDateTime.now());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Alertas de Segurança Ativos
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<SecurityAlert>> getActiveSecurityAlerts() {
        List<SecurityAlert> alerts = generateActiveSecurityAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Configurações de Segurança
     */
    @GetMapping("/config")
    public ResponseEntity<SecurityConfiguration> getSecurityConfiguration() {
        SecurityConfiguration config = new SecurityConfiguration();
        
        config.setPasswordPolicy(Map.of(
            "minLength", 8,
            "requireUppercase", true,
            "requireLowercase", true,
            "requireNumbers", true,
            "requireSpecialChars", true,
            "maxAge", 90
        ));
        
        config.setSessionPolicy(Map.of(
            "maxSessions", 1,
            "sessionTimeout", 30,
            "requireReauth", true
        ));
        
        config.setRateLimiting(Map.of(
            "loginAttempts", 5,
            "timeWindow", 15,
            "blockDuration", 30
        ));
        
        config.setEncryption(Map.of(
            "algorithm", "AES-256",
            "keyRotationDays", 90,
            "saltRounds", 12
        ));
        
        return ResponseEntity.ok(config);
    }

    /**
     * Atualizar Configurações de Segurança
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateSecurityConfiguration(
            @RequestBody SecurityConfiguration config) {
        
        // Simular atualização de configurações
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Security configuration updated successfully");
        response.put("updatedAt", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Executar Scan de Segurança Manual
     */
    @PostMapping("/scans/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> executeSecurity Scan(
            @RequestParam String scanType) {
        
        Map<String, String> response = new HashMap<>();
        
        if (!Arrays.asList("SAST", "DAST", "SCA", "FULL").contains(scanType)) {
            response.put("status", "error");
            response.put("message", "Invalid scan type. Supported: SAST, DAST, SCA, FULL");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Simular execução de scan
        String scanId = UUID.randomUUID().toString();
        response.put("status", "started");
        response.put("scanId", scanId);
        response.put("scanType", scanType);
        response.put("message", "Security scan started successfully");
        response.put("estimatedDuration", getEstimatedDuration(scanType));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Status de Scan de Segurança
     */
    @GetMapping("/scans/{scanId}/status")
    public ResponseEntity<SecurityScanStatus> getScanStatus(@PathVariable String scanId) {
        SecurityScanStatus status = new SecurityScanStatus();
        status.setScanId(scanId);
        status.setStatus("COMPLETED");
        status.setProgress(100);
        status.setStartedAt(LocalDateTime.now().minusMinutes(15));
        status.setCompletedAt(LocalDateTime.now());
        status.setVulnerabilitiesFound(12);
        status.setCriticalIssues(2);
        status.setHighIssues(4);
        status.setMediumIssues(6);
        
        return ResponseEntity.ok(status);
    }

    // Métodos auxiliares
    private SecurityStatus calculateOverallSecurityStatus(
            LGPDComplianceService.ComplianceMetrics lgpdMetrics,
            VulnerabilityManagementService.VulnerabilityMetrics vulnMetrics) {
        
        if (vulnMetrics.getCriticalCount() > 0 || lgpdMetrics.getConsentViolations() > 0) {
            return SecurityStatus.CRITICAL;
        }
        
        if (vulnMetrics.getHighSeverityCount() > 5 || lgpdMetrics.getDataMinimizationViolations() > 0) {
            return SecurityStatus.HIGH_RISK;
        }
        
        if (vulnMetrics.getMediumSeverityCount() > 10 || lgpdMetrics.getExpiredConsents() > lgpdMetrics.getTotalConsents() * 0.1) {
            return SecurityStatus.MEDIUM_RISK;
        }
        
        return SecurityStatus.SECURE;
    }

    private List<SecurityAlert> generateCriticalAlerts(
            LGPDComplianceService.ComplianceMetrics lgpdMetrics,
            VulnerabilityManagementService.VulnerabilityMetrics vulnMetrics) {
        
        List<SecurityAlert> alerts = new ArrayList<>();
        
        if (vulnMetrics.getCriticalCount() > 0) {
            alerts.add(new SecurityAlert(
                "CRITICAL_VULNERABILITIES",
                "Critical vulnerabilities detected: " + vulnMetrics.getCriticalCount(),
                AlertSeverity.CRITICAL,
                LocalDateTime.now()
            ));
        }
        
        if (lgpdMetrics.getConsentViolations() > 0) {
            alerts.add(new SecurityAlert(
                "LGPD_VIOLATIONS",
                "LGPD consent violations detected: " + lgpdMetrics.getConsentViolations(),
                AlertSeverity.HIGH,
                LocalDateTime.now()
            ));
        }
        
        if (lgpdMetrics.getPendingRequests() > 10) {
            alerts.add(new SecurityAlert(
                "PENDING_DATA_REQUESTS",
                "High number of pending data subject requests: " + lgpdMetrics.getPendingRequests(),
                AlertSeverity.MEDIUM,
                LocalDateTime.now()
            ));
        }
        
        return alerts;
    }

    private Map<String, Object> generateSecurityTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // Simular dados de tendência dos últimos 30 dias
        trends.put("vulnerabilityTrend", Arrays.asList(15, 12, 18, 10, 8, 14, 9));
        trends.put("complianceScore", Arrays.asList(85, 87, 89, 92, 94, 96, 98));
        trends.put("securityIncidents", Arrays.asList(3, 2, 1, 0, 1, 0, 0));
        trends.put("consentRate", Arrays.asList(78, 82, 85, 88, 91, 94, 96));
        
        return trends;
    }

    private List<Map<String, Object>> generateMockDataSubjectRequests(int page, int size, String status) {
        List<Map<String, Object>> requests = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> request = new HashMap<>();
            request.put("id", UUID.randomUUID().toString());
            request.put("userId", "user" + (i + page * size));
            request.put("type", Arrays.asList("PORTABILITY", "DELETION", "RECTIFICATION").get(i % 3));
            request.put("status", status != null ? status : Arrays.asList("PENDING", "COMPLETED", "FAILED").get(i % 3));
            request.put("requestedAt", LocalDateTime.now().minusDays(i));
            requests.add(request);
        }
        
        return requests;
    }

    private List<SecurityScanResult> generateMockScanHistory(int days) {
        List<SecurityScanResult> history = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            SecurityScanResult scan = new SecurityScanResult();
            scan.setId(UUID.randomUUID().toString());
            scan.setType(Arrays.asList("SAST", "DAST", "SCA").get(i % 3));
            scan.setExecutedAt(LocalDateTime.now().minusDays(i));
            scan.setDuration(15 + (int)(Math.random() * 30));
            scan.setVulnerabilitiesFound((int)(Math.random() * 20));
            scan.setStatus("COMPLETED");
            history.add(scan);
        }
        
        return history;
    }

    private List<SecurityAlert> generateActiveSecurityAlerts() {
        List<SecurityAlert> alerts = new ArrayList<>();
        
        alerts.add(new SecurityAlert(
            "HIGH_FAILED_LOGINS",
            "Unusual number of failed login attempts detected",
            AlertSeverity.MEDIUM,
            LocalDateTime.now().minusHours(2)
        ));
        
        alerts.add(new SecurityAlert(
            "OUTDATED_DEPENDENCIES",
            "5 dependencies with known vulnerabilities detected",
            AlertSeverity.HIGH,
            LocalDateTime.now().minusHours(6)
        ));
        
        return alerts;
    }

    private String getEstimatedDuration(String scanType) {
        return switch (scanType) {
            case "SAST" -> "10-15 minutes";
            case "DAST" -> "30-45 minutes";
            case "SCA" -> "5-10 minutes";
            case "FULL" -> "45-60 minutes";
            default -> "Unknown";
        };
    }

    // Classes de dados para o dashboard
    public static class SecurityDashboard {
        private LGPDComplianceService.ComplianceMetrics lgpdMetrics;
        private VulnerabilityManagementService.VulnerabilityMetrics vulnerabilityMetrics;
        private SecurityStatus overallSecurityStatus;
        private List<SecurityAlert> criticalAlerts;
        private Map<String, Object> securityTrends;
        private LocalDateTime lastUpdated;

        // Getters e Setters
        public LGPDComplianceService.ComplianceMetrics getLgpdMetrics() { return lgpdMetrics; }
        public void setLgpdMetrics(LGPDComplianceService.ComplianceMetrics lgpdMetrics) { this.lgpdMetrics = lgpdMetrics; }
        public VulnerabilityManagementService.VulnerabilityMetrics getVulnerabilityMetrics() { return vulnerabilityMetrics; }
        public void setVulnerabilityMetrics(VulnerabilityManagementService.VulnerabilityMetrics vulnerabilityMetrics) { this.vulnerabilityMetrics = vulnerabilityMetrics; }
        public SecurityStatus getOverallSecurityStatus() { return overallSecurityStatus; }
        public void setOverallSecurityStatus(SecurityStatus overallSecurityStatus) { this.overallSecurityStatus = overallSecurityStatus; }
        public List<SecurityAlert> getCriticalAlerts() { return criticalAlerts; }
        public void setCriticalAlerts(List<SecurityAlert> criticalAlerts) { this.criticalAlerts = criticalAlerts; }
        public Map<String, Object> getSecurityTrends() { return securityTrends; }
        public void setSecurityTrends(Map<String, Object> securityTrends) { this.securityTrends = securityTrends; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class SecurityAlert {
        private String type;
        private String message;
        private AlertSeverity severity;
        private LocalDateTime timestamp;

        public SecurityAlert(String type, String message, AlertSeverity severity, LocalDateTime timestamp) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
        }

        // Getters e Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public AlertSeverity getSeverity() { return severity; }
        public void setSeverity(AlertSeverity severity) { this.severity = severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class SecurityPerformanceMetrics {
        private double averageAuthenticationTime;
        private double averageAuthorizationTime;
        private double securityValidationTime;
        private long totalSecurityChecks;
        private long failedSecurityChecks;
        private double securityCheckSuccessRate;
        private LocalDateTime lastCalculated;

        // Getters e Setters
        public double getAverageAuthenticationTime() { return averageAuthenticationTime; }
        public void setAverageAuthenticationTime(double averageAuthenticationTime) { this.averageAuthenticationTime = averageAuthenticationTime; }
        public double getAverageAuthorizationTime() { return averageAuthorizationTime; }
        public void setAverageAuthorizationTime(double averageAuthorizationTime) { this.averageAuthorizationTime = averageAuthorizationTime; }
        public double getSecurityValidationTime() { return securityValidationTime; }
        public void setSecurityValidationTime(double securityValidationTime) { this.securityValidationTime = securityValidationTime; }
        public long getTotalSecurityChecks() { return totalSecurityChecks; }
        public void setTotalSecurityChecks(long totalSecurityChecks) { this.totalSecurityChecks = totalSecurityChecks; }
        public long getFailedSecurityChecks() { return failedSecurityChecks; }
        public void setFailedSecurityChecks(long failedSecurityChecks) { this.failedSecurityChecks = failedSecurityChecks; }
        public double getSecurityCheckSuccessRate() { return securityCheckSuccessRate; }
        public void setSecurityCheckSuccessRate(double securityCheckSuccessRate) { this.securityCheckSuccessRate = securityCheckSuccessRate; }
        public LocalDateTime getLastCalculated() { return lastCalculated; }
        public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }
    }

    public static class SecurityConfiguration {
        private Map<String, Object> passwordPolicy;
        private Map<String, Object> sessionPolicy;
        private Map<String, Object> rateLimiting;
        private Map<String, Object> encryption;

        // Getters e Setters
        public Map<String, Object> getPasswordPolicy() { return passwordPolicy; }
        public void setPasswordPolicy(Map<String, Object> passwordPolicy) { this.passwordPolicy = passwordPolicy; }
        public Map<String, Object> getSessionPolicy() { return sessionPolicy; }
        public void setSessionPolicy(Map<String, Object> sessionPolicy) { this.sessionPolicy = sessionPolicy; }
        public Map<String, Object> getRateLimiting() { return rateLimiting; }
        public void setRateLimiting(Map<String, Object> rateLimiting) { this.rateLimiting = rateLimiting; }
        public Map<String, Object> getEncryption() { return encryption; }
        public void setEncryption(Map<String, Object> encryption) { this.encryption = encryption; }
    }

    public static class SecurityScanResult {
        private String id;
        private String type;
        private LocalDateTime executedAt;
        private int duration;
        private int vulnerabilitiesFound;
        private String status;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getExecutedAt() { return executedAt; }
        public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public int getVulnerabilitiesFound() { return vulnerabilitiesFound; }
        public void setVulnerabilitiesFound(int vulnerabilitiesFound) { this.vulnerabilitiesFound = vulnerabilitiesFound; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class SecurityScanStatus {
        private String scanId;
        private String status;
        private int progress;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private int vulnerabilitiesFound;
        private int criticalIssues;
        private int highIssues;
        private int mediumIssues;

        // Getters e Setters
        public String getScanId() { return scanId; }
        public void setScanId(String scanId) { this.scanId = scanId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public int getVulnerabilitiesFound() { return vulnerabilitiesFound; }
        public void setVulnerabilitiesFound(int vulnerabilitiesFound) { this.vulnerabilitiesFound = vulnerabilitiesFound; }
        public int getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }
        public int getHighIssues() { return highIssues; }
        public void setHighIssues(int highIssues) { this.highIssues = highIssues; }
        public int getMediumIssues() { return mediumIssues; }
        public void setMediumIssues(int mediumIssues) { this.mediumIssues = mediumIssues; }
    }

    public enum SecurityStatus {
        SECURE, MEDIUM_RISK, HIGH_RISK, CRITICAL
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}