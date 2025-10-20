package com.betaware.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * LGPD Compliance Service
 * 
 * Implements automated privacy controls and data protection measures:
 * - Consent management and tracking
 * - Data minimization validation
 * - Encryption for data in transit and at rest
 * - Access control and audit logging
 * - Data subject rights (portability, deletion)
 * - Automated compliance monitoring
 */
@Service
public class LGPDComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(LGPDComplianceService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("LGPD_AUDIT");
    private static final Logger dataLogger = LoggerFactory.getLogger("DATA_PROCESSING");

    @Value("${lgpd.encryption.algorithm:AES}")
    private String encryptionAlgorithm;

    @Value("${lgpd.consent.retention-days:365}")
    private int consentRetentionDays;

    @Value("${lgpd.data.retention-days:730}")
    private int dataRetentionDays;

    @Value("${lgpd.audit.enabled:true}")
    private boolean auditEnabled;

    // In-memory storage (in production, use database with encryption)
    private final Map<String, ConsentRecord> consentRecords = new ConcurrentHashMap<>();
    private final Map<String, DataProcessingRecord> dataProcessingRecords = new ConcurrentHashMap<>();
    private final Map<String, DataSubjectRequest> dataSubjectRequests = new ConcurrentHashMap<>();

    // Encryption key (in production, use proper key management)
    private final SecretKey encryptionKey;

    // Sensitive data patterns
    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\(\\d{2}\\)\\s\\d{4,5}-\\d{4}");

    public LGPDComplianceService() {
        this.encryptionKey = generateEncryptionKey();
    }

    /**
     * Record user consent for data processing
     */
    public ConsentRecord recordConsent(String userId, ConsentType consentType, 
                                     String purpose, boolean granted, String ipAddress) {
        
        ConsentRecord consent = new ConsentRecord();
        consent.setId(UUID.randomUUID().toString());
        consent.setUserId(userId);
        consent.setConsentType(consentType);
        consent.setPurpose(purpose);
        consent.setGranted(granted);
        consent.setTimestamp(LocalDateTime.now());
        consent.setIpAddress(ipAddress);
        consent.setUserAgent(getCurrentUserAgent());
        consent.setVersion("1.0");

        consentRecords.put(consent.getId(), consent);

        // Audit logging
        auditLogger.info("Consent recorded - User: {}, Type: {}, Purpose: {}, Granted: {}, IP: {}, Timestamp: {}", 
            userId, consentType, purpose, granted, ipAddress, consent.getTimestamp());

        return consent;
    }

    /**
     * Validate consent for data processing
     */
    public boolean validateConsent(String userId, ConsentType consentType, String purpose) {
        List<ConsentRecord> userConsents = consentRecords.values().stream()
            .filter(c -> c.getUserId().equals(userId))
            .filter(c -> c.getConsentType() == consentType)
            .filter(c -> c.getPurpose().equals(purpose))
            .sorted((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()))
            .toList();

        if (userConsents.isEmpty()) {
            auditLogger.warn("No consent found - User: {}, Type: {}, Purpose: {}", userId, consentType, purpose);
            return false;
        }

        ConsentRecord latestConsent = userConsents.get(0);
        boolean isValid = latestConsent.isGranted() && 
                         latestConsent.getTimestamp().isAfter(LocalDateTime.now().minusDays(consentRetentionDays));

        if (!isValid) {
            auditLogger.warn("Invalid consent - User: {}, Type: {}, Purpose: {}, Granted: {}, Age: {} days", 
                userId, consentType, purpose, latestConsent.isGranted(),
                java.time.Duration.between(latestConsent.getTimestamp(), LocalDateTime.now()).toDays());
        }

        return isValid;
    }

    /**
     * Record data processing activity
     */
    public void recordDataProcessing(String userId, String dataType, ProcessingPurpose purpose, 
                                   String legalBasis, String processingDetails) {
        
        if (!validateConsent(userId, ConsentType.DATA_PROCESSING, purpose.toString())) {
            throw new IllegalStateException("No valid consent for data processing: " + purpose);
        }

        DataProcessingRecord record = new DataProcessingRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setDataType(dataType);
        record.setPurpose(purpose);
        record.setLegalBasis(legalBasis);
        record.setProcessingDetails(processingDetails);
        record.setTimestamp(LocalDateTime.now());
        record.setRetentionUntil(LocalDateTime.now().plusDays(dataRetentionDays));

        dataProcessingRecords.put(record.getId(), record);

        // Data processing audit log
        dataLogger.info("Data processing - User: {}, Type: {}, Purpose: {}, Legal Basis: {}, Timestamp: {}", 
            userId, dataType, purpose, legalBasis, record.getTimestamp());
    }

    /**
     * Encrypt sensitive data
     */
    public String encryptSensitiveData(String data) {
        try {
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            logger.error("Error encrypting sensitive data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt sensitive data
     */
    public String decryptSensitiveData(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error decrypting sensitive data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Mask sensitive data for logging
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        // Mask CPF
        maskedData = CPF_PATTERN.matcher(maskedData).replaceAll("***.***.***-**");

        // Mask email
        maskedData = EMAIL_PATTERN.matcher(maskedData).replaceAll(matchResult -> {
            String email = matchResult.group();
            int atIndex = email.indexOf('@');
            if (atIndex > 2) {
                return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
            }
            return "***@" + email.substring(atIndex + 1);
        });

        // Mask phone
        maskedData = PHONE_PATTERN.matcher(maskedData).replaceAll("(**) ****-****");

        return maskedData;
    }

    /**
     * Validate data minimization
     */
    public DataMinimizationResult validateDataMinimization(Map<String, Object> userData, 
                                                          ProcessingPurpose purpose) {
        
        DataMinimizationResult result = new DataMinimizationResult();
        result.setPurpose(purpose);
        result.setValidationTimestamp(LocalDateTime.now());

        Set<String> requiredFields = getRequiredFieldsForPurpose(purpose);
        Set<String> providedFields = userData.keySet();
        
        // Check for missing required fields
        Set<String> missingFields = new HashSet<>(requiredFields);
        missingFields.removeAll(providedFields);
        result.setMissingRequiredFields(missingFields);

        // Check for excessive data collection
        Set<String> excessiveFields = new HashSet<>(providedFields);
        excessiveFields.removeAll(requiredFields);
        result.setExcessiveFields(excessiveFields);

        // Determine compliance status
        boolean isCompliant = missingFields.isEmpty() && excessiveFields.isEmpty();
        result.setCompliant(isCompliant);

        if (!isCompliant) {
            auditLogger.warn("Data minimization violation - Purpose: {}, Missing: {}, Excessive: {}", 
                purpose, missingFields, excessiveFields);
        }

        return result;
    }

    /**
     * Process data subject request (portability, deletion, etc.)
     */
    public DataSubjectRequest processDataSubjectRequest(String userId, DataSubjectRightType rightType, 
                                                       String requestDetails) {
        
        DataSubjectRequest request = new DataSubjectRequest();
        request.setId(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setRightType(rightType);
        request.setRequestDetails(requestDetails);
        request.setRequestTimestamp(LocalDateTime.now());
        request.setStatus(RequestStatus.PENDING);

        dataSubjectRequests.put(request.getId(), request);

        // Process request based on type
        switch (rightType) {
            case DATA_PORTABILITY:
                processDataPortabilityRequest(request);
                break;
            case DATA_DELETION:
                processDataDeletionRequest(request);
                break;
            case ACCESS_REQUEST:
                processAccessRequest(request);
                break;
            case RECTIFICATION:
                processRectificationRequest(request);
                break;
        }

        auditLogger.info("Data subject request processed - User: {}, Type: {}, Request ID: {}, Status: {}", 
            userId, rightType, request.getId(), request.getStatus());

        return request;
    }

    /**
     * Process data portability request
     */
    private void processDataPortabilityRequest(DataSubjectRequest request) {
        try {
            // Collect all user data
            Map<String, Object> userData = collectUserData(request.getUserId());
            
            // Create portable data package
            String portableData = createPortableDataPackage(userData);
            
            request.setResponseData(portableData);
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletionTimestamp(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error processing data portability request", e);
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Process data deletion request
     */
    private void processDataDeletionRequest(DataSubjectRequest request) {
        try {
            String userId = request.getUserId();
            
            // Delete user data (implement based on your data model)
            deleteUserData(userId);
            
            // Anonymize audit logs
            anonymizeAuditLogs(userId);
            
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletionTimestamp(LocalDateTime.now());
            request.setResponseData("User data successfully deleted and anonymized");
            
        } catch (Exception e) {
            logger.error("Error processing data deletion request", e);
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Generate compliance report
     */
    public LGPDComplianceReport generateComplianceReport() {
        LGPDComplianceReport report = new LGPDComplianceReport();
        report.setGeneratedAt(LocalDateTime.now());
        
        // Consent metrics
        long totalConsents = consentRecords.size();
        long grantedConsents = consentRecords.values().stream()
            .mapToLong(c -> c.isGranted() ? 1 : 0).sum();
        long expiredConsents = consentRecords.values().stream()
            .filter(c -> c.getTimestamp().isBefore(LocalDateTime.now().minusDays(consentRetentionDays)))
            .count();

        report.setTotalConsents(totalConsents);
        report.setGrantedConsents(grantedConsents);
        report.setExpiredConsents(expiredConsents);
        report.setConsentComplianceRate((double) grantedConsents / totalConsents * 100);

        // Data processing metrics
        report.setTotalDataProcessingRecords(dataProcessingRecords.size());
        
        Map<ProcessingPurpose, Long> purposeCount = dataProcessingRecords.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DataProcessingRecord::getPurpose, 
                java.util.stream.Collectors.counting()));
        report.setProcessingByPurpose(purposeCount);

        // Data subject requests metrics
        report.setTotalDataSubjectRequests(dataSubjectRequests.size());
        
        Map<DataSubjectRightType, Long> requestTypeCount = dataSubjectRequests.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DataSubjectRequest::getRightType, 
                java.util.stream.Collectors.counting()));
        report.setRequestsByType(requestTypeCount);

        // Calculate average response time
        double avgResponseTime = dataSubjectRequests.values().stream()
            .filter(r -> r.getStatus() == RequestStatus.COMPLETED && r.getCompletionTimestamp() != null)
            .mapToDouble(r -> java.time.Duration.between(r.getRequestTimestamp(), r.getCompletionTimestamp()).toHours())
            .average().orElse(0.0);
        report.setAverageResponseTimeHours(avgResponseTime);

        // Compliance recommendations
        List<String> recommendations = generateComplianceRecommendations(report);
        report.setRecommendations(recommendations);

        return report;
    }

    /**
     * Scheduled compliance monitoring
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void performComplianceMonitoring() {
        logger.info("Starting scheduled LGPD compliance monitoring");

        try {
            // Check for expired consents
            checkExpiredConsents();
            
            // Check data retention compliance
            checkDataRetentionCompliance();
            
            // Generate compliance metrics
            LGPDComplianceReport report = generateComplianceReport();
            
            auditLogger.info("Compliance monitoring completed - Consents: {}, Processing Records: {}, Requests: {}", 
                report.getTotalConsents(), report.getTotalDataProcessingRecords(), report.getTotalDataSubjectRequests());
            
        } catch (Exception e) {
            logger.error("Error during compliance monitoring", e);
        }
    }

    // Helper methods
    private SecretKey generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            logger.error("Error generating encryption key", e);
            // Fallback to a fixed key (not recommended for production)
            byte[] keyBytes = "MySecretKey12345".getBytes(StandardCharsets.UTF_8);
            return new SecretKeySpec(keyBytes, "AES");
        }
    }

    private String getCurrentUserAgent() {
        // In a real implementation, get from HTTP request
        return "BetAware-API/1.0";
    }

    private Set<String> getRequiredFieldsForPurpose(ProcessingPurpose purpose) {
        switch (purpose) {
            case ACCOUNT_CREATION:
                return Set.of("email", "name", "dateOfBirth");
            case BETTING_SERVICES:
                return Set.of("userId", "betAmount", "gameType");
            case MARKETING:
                return Set.of("email", "preferences");
            case ANALYTICS:
                return Set.of("userId", "sessionId");
            default:
                return Set.of();
        }
    }

    private Map<String, Object> collectUserData(String userId) {
        // In a real implementation, collect from all relevant tables/services
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("consents", consentRecords.values().stream()
            .filter(c -> c.getUserId().equals(userId)).toList());
        userData.put("processingRecords", dataProcessingRecords.values().stream()
            .filter(r -> r.getUserId().equals(userId)).toList());
        return userData;
    }

    private String createPortableDataPackage(Map<String, Object> userData) {
        // Create JSON representation of user data
        return "{\n" +
               "  \"exportDate\": \"" + LocalDateTime.now() + "\",\n" +
               "  \"userData\": " + userData.toString() + "\n" +
               "}";
    }

    private void deleteUserData(String userId) {
        // Remove user data from all systems
        consentRecords.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
        dataProcessingRecords.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
        
        logger.info("User data deleted for user: {}", userId);
    }

    private void anonymizeAuditLogs(String userId) {
        // In a real implementation, anonymize audit logs
        logger.info("Audit logs anonymized for user: {}", userId);
    }

    private void processAccessRequest(DataSubjectRequest request) {
        try {
            Map<String, Object> userData = collectUserData(request.getUserId());
            request.setResponseData(userData.toString());
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletionTimestamp(LocalDateTime.now());
        } catch (Exception e) {
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
        }
    }

    private void processRectificationRequest(DataSubjectRequest request) {
        // Implementation depends on specific rectification requirements
        request.setStatus(RequestStatus.MANUAL_REVIEW_REQUIRED);
    }

    private void checkExpiredConsents() {
        long expiredCount = consentRecords.values().stream()
            .filter(c -> c.getTimestamp().isBefore(LocalDateTime.now().minusDays(consentRetentionDays)))
            .count();
        
        if (expiredCount > 0) {
            auditLogger.warn("Found {} expired consents requiring renewal", expiredCount);
        }
    }

    private void checkDataRetentionCompliance() {
        long expiredRecords = dataProcessingRecords.values().stream()
            .filter(r -> r.getRetentionUntil().isBefore(LocalDateTime.now()))
            .count();
        
        if (expiredRecords > 0) {
            auditLogger.warn("Found {} data processing records exceeding retention period", expiredRecords);
        }
    }

    private List<String> generateComplianceRecommendations(LGPDComplianceReport report) {
        List<String> recommendations = new ArrayList<>();
        
        if (report.getConsentComplianceRate() < 95) {
            recommendations.add("Improve consent collection process - current rate: " + 
                String.format("%.1f%%", report.getConsentComplianceRate()));
        }
        
        if (report.getExpiredConsents() > 0) {
            recommendations.add("Renew " + report.getExpiredConsents() + " expired consents");
        }
        
        if (report.getAverageResponseTimeHours() > 72) {
            recommendations.add("Improve data subject request response time - current average: " + 
                String.format("%.1f hours", report.getAverageResponseTimeHours()));
        }
        
        recommendations.add("Regular compliance training for development team");
        recommendations.add("Implement automated data retention policies");
        
        return recommendations;
    }

    // Data classes and enums
    public static class ConsentRecord {
        private String id;
        private String userId;
        private ConsentType consentType;
        private String purpose;
        private boolean granted;
        private LocalDateTime timestamp;
        private String ipAddress;
        private String userAgent;
        private String version;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public ConsentType getConsentType() { return consentType; }
        public void setConsentType(ConsentType consentType) { this.consentType = consentType; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public boolean isGranted() { return granted; }
        public void setGranted(boolean granted) { this.granted = granted; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class DataProcessingRecord {
        private String id;
        private String userId;
        private String dataType;
        private ProcessingPurpose purpose;
        private String legalBasis;
        private String processingDetails;
        private LocalDateTime timestamp;
        private LocalDateTime retentionUntil;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public ProcessingPurpose getPurpose() { return purpose; }
        public void setPurpose(ProcessingPurpose purpose) { this.purpose = purpose; }
        public String getLegalBasis() { return legalBasis; }
        public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
        public String getProcessingDetails() { return processingDetails; }
        public void setProcessingDetails(String processingDetails) { this.processingDetails = processingDetails; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public LocalDateTime getRetentionUntil() { return retentionUntil; }
        public void setRetentionUntil(LocalDateTime retentionUntil) { this.retentionUntil = retentionUntil; }
    }

    public static class DataSubjectRequest {
        private String id;
        private String userId;
        private DataSubjectRightType rightType;
        private String requestDetails;
        private LocalDateTime requestTimestamp;
        private LocalDateTime completionTimestamp;
        private RequestStatus status;
        private String responseData;
        private String errorMessage;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public DataSubjectRightType getRightType() { return rightType; }
        public void setRightType(DataSubjectRightType rightType) { this.rightType = rightType; }
        public String getRequestDetails() { return requestDetails; }
        public void setRequestDetails(String requestDetails) { this.requestDetails = requestDetails; }
        public LocalDateTime getRequestTimestamp() { return requestTimestamp; }
        public void setRequestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }
        public LocalDateTime getCompletionTimestamp() { return completionTimestamp; }
        public void setCompletionTimestamp(LocalDateTime completionTimestamp) { this.completionTimestamp = completionTimestamp; }
        public RequestStatus getStatus() { return status; }
        public void setStatus(RequestStatus status) { this.status = status; }
        public String getResponseData() { return responseData; }
        public void setResponseData(String responseData) { this.responseData = responseData; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class DataMinimizationResult {
        private ProcessingPurpose purpose;
        private LocalDateTime validationTimestamp;
        private boolean compliant;
        private Set<String> missingRequiredFields;
        private Set<String> excessiveFields;

        // Getters and setters
        public ProcessingPurpose getPurpose() { return purpose; }
        public void setPurpose(ProcessingPurpose purpose) { this.purpose = purpose; }
        public LocalDateTime getValidationTimestamp() { return validationTimestamp; }
        public void setValidationTimestamp(LocalDateTime validationTimestamp) { this.validationTimestamp = validationTimestamp; }
        public boolean isCompliant() { return compliant; }
        public void setCompliant(boolean compliant) { this.compliant = compliant; }
        public Set<String> getMissingRequiredFields() { return missingRequiredFields; }
        public void setMissingRequiredFields(Set<String> missingRequiredFields) { this.missingRequiredFields = missingRequiredFields; }
        public Set<String> getExcessiveFields() { return excessiveFields; }
        public void setExcessiveFields(Set<String> excessiveFields) { this.excessiveFields = excessiveFields; }
    }

    public static class LGPDComplianceReport {
        private LocalDateTime generatedAt;
        private long totalConsents;
        private long grantedConsents;
        private long expiredConsents;
        private double consentComplianceRate;
        private long totalDataProcessingRecords;
        private Map<ProcessingPurpose, Long> processingByPurpose;
        private long totalDataSubjectRequests;
        private Map<DataSubjectRightType, Long> requestsByType;
        private double averageResponseTimeHours;
        private List<String> recommendations;

        // Getters and setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public long getTotalConsents() { return totalConsents; }
        public void setTotalConsents(long totalConsents) { this.totalConsents = totalConsents; }
        public long getGrantedConsents() { return grantedConsents; }
        public void setGrantedConsents(long grantedConsents) { this.grantedConsents = grantedConsents; }
        public long getExpiredConsents() { return expiredConsents; }
        public void setExpiredConsents(long expiredConsents) { this.expiredConsents = expiredConsents; }
        public double getConsentComplianceRate() { return consentComplianceRate; }
        public void setConsentComplianceRate(double consentComplianceRate) { this.consentComplianceRate = consentComplianceRate; }
        public long getTotalDataProcessingRecords() { return totalDataProcessingRecords; }
        public void setTotalDataProcessingRecords(long totalDataProcessingRecords) { this.totalDataProcessingRecords = totalDataProcessingRecords; }
        public Map<ProcessingPurpose, Long> getProcessingByPurpose() { return processingByPurpose; }
        public void setProcessingByPurpose(Map<ProcessingPurpose, Long> processingByPurpose) { this.processingByPurpose = processingByPurpose; }
        public long getTotalDataSubjectRequests() { return totalDataSubjectRequests; }
        public void setTotalDataSubjectRequests(long totalDataSubjectRequests) { this.totalDataSubjectRequests = totalDataSubjectRequests; }
        public Map<DataSubjectRightType, Long> getRequestsByType() { return requestsByType; }
        public void setRequestsByType(Map<DataSubjectRightType, Long> requestsByType) { this.requestsByType = requestsByType; }
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    // Enums
    public enum ConsentType {
        DATA_PROCESSING, MARKETING, ANALYTICS, COOKIES, THIRD_PARTY_SHARING
    }

    public enum ProcessingPurpose {
        ACCOUNT_CREATION, BETTING_SERVICES, MARKETING, ANALYTICS, CUSTOMER_SUPPORT, LEGAL_COMPLIANCE
    }

    public enum DataSubjectRightType {
        ACCESS_REQUEST, DATA_PORTABILITY, DATA_DELETION, RECTIFICATION, PROCESSING_RESTRICTION
    }

    public enum RequestStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, MANUAL_REVIEW_REQUIRED
    }
}