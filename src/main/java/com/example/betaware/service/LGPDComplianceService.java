package com.example.betaware.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço de Conformidade LGPD
 * Implementa controles automatizados de privacidade e proteção de dados
 */
@Service
public class LGPDComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(LGPDComplianceService.class);
    
    private final Map<String, ConsentRecord> consentRecords = new ConcurrentHashMap<>();
    private final Map<String, DataProcessingRecord> processingRecords = new ConcurrentHashMap<>();
    private final Map<String, DataSubjectRequest> dataSubjectRequests = new ConcurrentHashMap<>();
    private final ComplianceMetrics metrics = new ComplianceMetrics();
    private final SecretKey encryptionKey;

    public LGPDComplianceService() {
        this.encryptionKey = generateEncryptionKey();
    }

    /**
     * Gerenciamento de Consentimento
     */
    public String recordConsent(String userId, String purpose, String legalBasis, boolean granted) {
        ConsentRecord consent = new ConsentRecord();
        consent.setId(UUID.randomUUID().toString());
        consent.setUserId(userId);
        consent.setPurpose(purpose);
        consent.setLegalBasis(legalBasis);
        consent.setGranted(granted);
        consent.setTimestamp(LocalDateTime.now());
        consent.setIpAddress(getCurrentUserIP());
        consent.setUserAgent(getCurrentUserAgent());
        
        consentRecords.put(consent.getId(), consent);
        
        logger.info("LGPD_AUDIT: Consentimento registrado - Usuário: {}, Finalidade: {}, Concedido: {}", 
                   userId, purpose, granted);
        
        updateComplianceMetrics();
        return consent.getId();
    }

    public boolean validateConsent(String userId, String purpose) {
        List<ConsentRecord> userConsents = consentRecords.values().stream()
                .filter(c -> c.getUserId().equals(userId) && c.getPurpose().equals(purpose))
                .sorted((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()))
                .collect(Collectors.toList());
        
        if (userConsents.isEmpty()) {
            logger.warn("LGPD_COMPLIANCE: Nenhum consentimento encontrado - Usuário: {}, Finalidade: {}", 
                       userId, purpose);
            return false;
        }
        
        ConsentRecord latestConsent = userConsents.get(0);
        boolean isValid = latestConsent.isGranted() && !isConsentExpired(latestConsent);
        
        if (!isValid) {
            logger.warn("LGPD_COMPLIANCE: Consentimento inválido ou expirado - Usuário: {}, Finalidade: {}", 
                       userId, purpose);
        }
        
        return isValid;
    }

    private boolean isConsentExpired(ConsentRecord consent) {
        // Consentimento expira após 2 anos (conforme LGPD)
        return consent.getTimestamp().plusYears(2).isBefore(LocalDateTime.now());
    }

    public void revokeConsent(String userId, String purpose) {
        recordConsent(userId, purpose, "REVOGAÇÃO", false);
        logger.info("LGPD_AUDIT: Consentimento revogado - Usuário: {}, Finalidade: {}", userId, purpose);
    }

    /**
     * Minimização de Dados
     */
    public boolean validateDataMinimization(String purpose, Set<String> dataFields) {
        Set<String> allowedFields = getAllowedFieldsForPurpose(purpose);
        
        boolean isCompliant = allowedFields.containsAll(dataFields);
        
        if (!isCompliant) {
            Set<String> excessiveFields = new HashSet<>(dataFields);
            excessiveFields.removeAll(allowedFields);
            
            logger.warn("LGPD_COMPLIANCE: Violação de minimização de dados - Finalidade: {}, Campos excessivos: {}", 
                       purpose, excessiveFields);
        }
        
        return isCompliant;
    }

    private Set<String> getAllowedFieldsForPurpose(String purpose) {
        Map<String, Set<String>> purposeFieldsMap = Map.of(
            "CADASTRO", Set.of("nome", "email", "telefone", "cpf"),
            "MARKETING", Set.of("nome", "email", "preferencias"),
            "SUPORTE", Set.of("nome", "email", "telefone", "historico_atendimento"),
            "APOSTAS", Set.of("nome", "cpf", "conta_bancaria", "historico_apostas"),
            "COMPLIANCE", Set.of("nome", "cpf", "endereco", "renda", "historico_transacoes")
        );
        
        return purposeFieldsMap.getOrDefault(purpose, Set.of());
    }

    /**
     * Criptografia de Dados Sensíveis
     */
    public String encryptSensitiveData(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            logger.error("Erro ao criptografar dados sensíveis", e);
            throw new RuntimeException("Falha na criptografia de dados", e);
        }
    }

    public String decryptSensitiveData(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Erro ao descriptografar dados sensíveis", e);
            throw new RuntimeException("Falha na descriptografia de dados", e);
        }
    }

    /**
     * Controle de Acesso a Dados
     */
    public boolean authorizeDataAccess(String userId, String requestedUserId, String purpose, String userRole) {
        // Usuário pode acessar seus próprios dados
        if (userId.equals(requestedUserId)) {
            return validateConsent(requestedUserId, purpose);
        }
        
        // Verificar permissões baseadas em papel
        boolean hasRolePermission = checkRolePermission(userRole, purpose);
        
        if (!hasRolePermission) {
            logger.warn("LGPD_AUDIT: Acesso negado - Usuário: {}, Papel: {}, Finalidade: {}", 
                       userId, userRole, purpose);
            return false;
        }
        
        // Registrar acesso para auditoria
        recordDataAccess(userId, requestedUserId, purpose, userRole);
        
        return true;
    }

    private boolean checkRolePermission(String role, String purpose) {
        Map<String, Set<String>> rolePermissions = Map.of(
            "ADMIN", Set.of("CADASTRO", "MARKETING", "SUPORTE", "APOSTAS", "COMPLIANCE"),
            "SUPORTE", Set.of("CADASTRO", "SUPORTE"),
            "MARKETING", Set.of("MARKETING"),
            "COMPLIANCE", Set.of("COMPLIANCE", "APOSTAS"),
            "USER", Set.of()
        );
        
        return rolePermissions.getOrDefault(role, Set.of()).contains(purpose);
    }

    private void recordDataAccess(String accessorUserId, String targetUserId, String purpose, String role) {
        DataProcessingRecord record = new DataProcessingRecord();
        record.setId(UUID.randomUUID().toString());
        record.setAccessorUserId(accessorUserId);
        record.setTargetUserId(targetUserId);
        record.setPurpose(purpose);
        record.setRole(role);
        record.setTimestamp(LocalDateTime.now());
        record.setIpAddress(getCurrentUserIP());
        
        processingRecords.put(record.getId(), record);
        
        logger.info("LGPD_AUDIT: Acesso a dados registrado - Accessor: {}, Alvo: {}, Finalidade: {}", 
                   accessorUserId, targetUserId, purpose);
    }

    /**
     * Direitos do Titular dos Dados
     */
    public String requestDataPortability(String userId) {
        String requestId = UUID.randomUUID().toString();
        
        DataSubjectRequest request = new DataSubjectRequest();
        request.setId(requestId);
        request.setUserId(userId);
        request.setType(RequestType.PORTABILITY);
        request.setStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        
        dataSubjectRequests.put(requestId, request);
        
        // Processar solicitação automaticamente
        processDataPortabilityRequest(request);
        
        logger.info("LGPD_AUDIT: Solicitação de portabilidade - Usuário: {}, Request ID: {}", userId, requestId);
        
        return requestId;
    }

    private void processDataPortabilityRequest(DataSubjectRequest request) {
        try {
            // Simular coleta de dados do usuário
            Map<String, Object> userData = collectUserData(request.getUserId());
            
            // Gerar arquivo JSON com os dados
            String dataPackage = generateDataPackage(userData);
            
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            request.setResult(dataPackage);
            
            logger.info("LGPD_AUDIT: Portabilidade concluída - Usuário: {}", request.getUserId());
            
        } catch (Exception e) {
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            logger.error("LGPD_ERROR: Falha na portabilidade - Usuário: {}", request.getUserId(), e);
        }
    }

    public String requestDataDeletion(String userId) {
        String requestId = UUID.randomUUID().toString();
        
        DataSubjectRequest request = new DataSubjectRequest();
        request.setId(requestId);
        request.setUserId(userId);
        request.setType(RequestType.DELETION);
        request.setStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        
        dataSubjectRequests.put(requestId, request);
        
        // Processar solicitação automaticamente
        processDataDeletionRequest(request);
        
        logger.info("LGPD_AUDIT: Solicitação de exclusão - Usuário: {}, Request ID: {}", userId, requestId);
        
        return requestId;
    }

    private void processDataDeletionRequest(DataSubjectRequest request) {
        try {
            // Verificar se há obrigações legais que impedem a exclusão
            if (hasLegalObligationToRetain(request.getUserId())) {
                request.setStatus(RequestStatus.REJECTED);
                request.setErrorMessage("Dados não podem ser excluídos devido a obrigações legais");
                logger.warn("LGPD_AUDIT: Exclusão rejeitada por obrigação legal - Usuário: {}", request.getUserId());
                return;
            }
            
            // Simular exclusão/anonimização de dados
            anonymizeUserData(request.getUserId());
            
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            
            logger.info("LGPD_AUDIT: Exclusão concluída - Usuário: {}", request.getUserId());
            
        } catch (Exception e) {
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            logger.error("LGPD_ERROR: Falha na exclusão - Usuário: {}", request.getUserId(), e);
        }
    }

    private boolean hasLegalObligationToRetain(String userId) {
        // Verificar se há apostas ativas, investigações em andamento, etc.
        // Para demonstração, simular algumas condições
        return Math.random() > 0.8; // 20% chance de ter obrigação legal
    }

    private void anonymizeUserData(String userId) {
        // Simular anonimização de dados
        logger.info("Anonimizando dados do usuário: {}", userId);
        
        // Em uma implementação real, isso envolveria:
        // 1. Substituir dados pessoais por valores anônimos
        // 2. Manter dados necessários para compliance (anonimizados)
        // 3. Atualizar todas as tabelas relacionadas
    }

    /**
     * Gestão de Retenção de Dados
     */
    public void enforceDataRetentionPolicies() {
        logger.info("Executando políticas de retenção de dados");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(5); // Retenção de 5 anos
        
        // Identificar dados expirados
        List<DataProcessingRecord> expiredRecords = processingRecords.values().stream()
                .filter(record -> record.getTimestamp().isBefore(cutoffDate))
                .collect(Collectors.toList());
        
        // Arquivar ou excluir dados expirados
        for (DataProcessingRecord record : expiredRecords) {
            archiveOrDeleteRecord(record);
        }
        
        logger.info("Políticas de retenção aplicadas: {} registros processados", expiredRecords.size());
    }

    private void archiveOrDeleteRecord(DataProcessingRecord record) {
        // Simular arquivamento ou exclusão
        processingRecords.remove(record.getId());
        logger.debug("Registro arquivado/excluído: {}", record.getId());
    }

    /**
     * Monitoramento de Conformidade
     */
    @Scheduled(cron = "0 0 1 * * ?") // 1:00 AM todos os dias
    public void performComplianceMonitoring() {
        logger.info("Iniciando monitoramento de conformidade LGPD");
        
        updateComplianceMetrics();
        generateComplianceReport();
        enforceDataRetentionPolicies();
        
        // Alertar sobre problemas de conformidade
        if (metrics.getConsentViolations() > 0) {
            logger.error("LGPD_ALERT: {} violações de consentimento detectadas!", metrics.getConsentViolations());
        }
        
        if (metrics.getDataMinimizationViolations() > 0) {
            logger.error("LGPD_ALERT: {} violações de minimização de dados detectadas!", 
                        metrics.getDataMinimizationViolations());
        }
    }

    private void updateComplianceMetrics() {
        metrics.setTotalConsents(consentRecords.size());
        metrics.setActiveConsents(countActiveConsents());
        metrics.setExpiredConsents(countExpiredConsents());
        metrics.setTotalDataRequests(dataSubjectRequests.size());
        metrics.setPendingRequests(countPendingRequests());
        metrics.setCompletedRequests(countCompletedRequests());
        metrics.setLastUpdated(LocalDateTime.now());
        
        // Calcular violações (simulado)
        metrics.setConsentViolations(calculateConsentViolations());
        metrics.setDataMinimizationViolations(calculateDataMinimizationViolations());
    }

    private long countActiveConsents() {
        return consentRecords.values().stream()
                .filter(c -> c.isGranted() && !isConsentExpired(c))
                .count();
    }

    private long countExpiredConsents() {
        return consentRecords.values().stream()
                .filter(this::isConsentExpired)
                .count();
    }

    private long countPendingRequests() {
        return dataSubjectRequests.values().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .count();
    }

    private long countCompletedRequests() {
        return dataSubjectRequests.values().stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED)
                .count();
    }

    private int calculateConsentViolations() {
        // Simular detecção de violações de consentimento
        return (int) (Math.random() * 3); // 0-2 violações
    }

    private int calculateDataMinimizationViolations() {
        // Simular detecção de violações de minimização
        return (int) (Math.random() * 2); // 0-1 violações
    }

    public ComplianceReport generateComplianceReport() {
        ComplianceReport report = new ComplianceReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setMetrics(metrics);
        
        // Gerar recomendações
        List<String> recommendations = generateComplianceRecommendations();
        report.setRecommendations(recommendations);
        
        // Status geral de conformidade
        report.setComplianceStatus(calculateOverallComplianceStatus());
        
        logger.info("Relatório de conformidade LGPD gerado: Status {}", report.getComplianceStatus());
        
        return report;
    }

    private List<String> generateComplianceRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        if (metrics.getExpiredConsents() > 0) {
            recommendations.add("Renovar " + metrics.getExpiredConsents() + " consentimentos expirados");
        }
        
        if (metrics.getPendingRequests() > 0) {
            recommendations.add("Processar " + metrics.getPendingRequests() + " solicitações pendentes de titulares");
        }
        
        if (metrics.getConsentViolations() > 0) {
            recommendations.add("Investigar e corrigir " + metrics.getConsentViolations() + " violações de consentimento");
        }
        
        double consentRate = (double) metrics.getActiveConsents() / metrics.getTotalConsents() * 100;
        if (consentRate < 80) {
            recommendations.add("Taxa de consentimento baixa (" + String.format("%.1f", consentRate) + 
                              "%). Revisar processo de obtenção de consentimento");
        }
        
        return recommendations;
    }

    private ComplianceStatus calculateOverallComplianceStatus() {
        if (metrics.getConsentViolations() > 0 || metrics.getDataMinimizationViolations() > 0) {
            return ComplianceStatus.NON_COMPLIANT;
        }
        
        if (metrics.getExpiredConsents() > metrics.getTotalConsents() * 0.1) {
            return ComplianceStatus.PARTIALLY_COMPLIANT;
        }
        
        return ComplianceStatus.COMPLIANT;
    }

    // Métodos auxiliares
    private String getCurrentUserIP() {
        // Em uma implementação real, obteria do contexto da requisição
        return "192.168.1.100";
    }

    private String getCurrentUserAgent() {
        // Em uma implementação real, obteria do contexto da requisição
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    }

    private SecretKey generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            // Fallback para chave fixa (apenas para demonstração)
            byte[] key = "MySecretKey12345".getBytes();
            return new SecretKeySpec(key, "AES");
        }
    }

    private Map<String, Object> collectUserData(String userId) {
        // Simular coleta de dados do usuário
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("nome", "João Silva");
        userData.put("email", "joao@example.com");
        userData.put("telefone", "(11) 99999-9999");
        userData.put("dataColeta", LocalDateTime.now());
        return userData;
    }

    private String generateDataPackage(Map<String, Object> userData) {
        // Simular geração de pacote de dados em JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        userData.forEach((key, value) -> 
            json.append("  \"").append(key).append("\": \"").append(value).append("\",\n"));
        json.append("  \"geradoEm\": \"").append(LocalDateTime.now()).append("\"\n");
        json.append("}");
        return json.toString();
    }

    // Getters para métricas e relatórios
    public ComplianceMetrics getMetrics() {
        return metrics;
    }

    public DataSubjectRequest getDataSubjectRequest(String requestId) {
        return dataSubjectRequests.get(requestId);
    }

    public List<ConsentRecord> getUserConsents(String userId) {
        return consentRecords.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    // Classes de dados
    public static class ConsentRecord {
        private String id;
        private String userId;
        private String purpose;
        private String legalBasis;
        private boolean granted;
        private LocalDateTime timestamp;
        private String ipAddress;
        private String userAgent;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getLegalBasis() { return legalBasis; }
        public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
        public boolean isGranted() { return granted; }
        public void setGranted(boolean granted) { this.granted = granted; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class DataProcessingRecord {
        private String id;
        private String accessorUserId;
        private String targetUserId;
        private String purpose;
        private String role;
        private LocalDateTime timestamp;
        private String ipAddress;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAccessorUserId() { return accessorUserId; }
        public void setAccessorUserId(String accessorUserId) { this.accessorUserId = accessorUserId; }
        public String getTargetUserId() { return targetUserId; }
        public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }

    public static class DataSubjectRequest {
        private String id;
        private String userId;
        private RequestType type;
        private RequestStatus status;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
        private String result;
        private String errorMessage;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public RequestType getType() { return type; }
        public void setType(RequestType type) { this.type = type; }
        public RequestStatus getStatus() { return status; }
        public void setStatus(RequestStatus status) { this.status = status; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class ComplianceMetrics {
        private int totalConsents;
        private long activeConsents;
        private long expiredConsents;
        private int totalDataRequests;
        private long pendingRequests;
        private long completedRequests;
        private int consentViolations;
        private int dataMinimizationViolations;
        private LocalDateTime lastUpdated;

        // Getters e Setters
        public int getTotalConsents() { return totalConsents; }
        public void setTotalConsents(int totalConsents) { this.totalConsents = totalConsents; }
        public long getActiveConsents() { return activeConsents; }
        public void setActiveConsents(long activeConsents) { this.activeConsents = activeConsents; }
        public long getExpiredConsents() { return expiredConsents; }
        public void setExpiredConsents(long expiredConsents) { this.expiredConsents = expiredConsents; }
        public int getTotalDataRequests() { return totalDataRequests; }
        public void setTotalDataRequests(int totalDataRequests) { this.totalDataRequests = totalDataRequests; }
        public long getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }
        public long getCompletedRequests() { return completedRequests; }
        public void setCompletedRequests(long completedRequests) { this.completedRequests = completedRequests; }
        public int getConsentViolations() { return consentViolations; }
        public void setConsentViolations(int consentViolations) { this.consentViolations = consentViolations; }
        public int getDataMinimizationViolations() { return dataMinimizationViolations; }
        public void setDataMinimizationViolations(int dataMinimizationViolations) { this.dataMinimizationViolations = dataMinimizationViolations; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class ComplianceReport {
        private LocalDateTime generatedAt;
        private ComplianceMetrics metrics;
        private List<String> recommendations;
        private ComplianceStatus complianceStatus;

        // Getters e Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public ComplianceMetrics getMetrics() { return metrics; }
        public void setMetrics(ComplianceMetrics metrics) { this.metrics = metrics; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public ComplianceStatus getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
    }

    public enum RequestType {
        PORTABILITY, DELETION, RECTIFICATION, ACCESS
    }

    public enum RequestStatus {
        PENDING, COMPLETED, FAILED, REJECTED
    }

    public enum ComplianceStatus {
        COMPLIANT, PARTIALLY_COMPLIANT, NON_COMPLIANT
    }
}