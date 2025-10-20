# BetAware API - Implementação de Cybersecurity

## 🔒 Sobre o Projeto de Cybersecurity
Este projeto implementa uma solução completa de segurança para a API BetAware, desenvolvida como parte da disciplina de **Cybersecurity**. O foco principal é demonstrar a aplicação prática de conceitos de segurança em aplicações Spring Boot, incluindo SSDLC (Secure Software Development Lifecycle), LGPD compliance, e integração de ferramentas de segurança automatizadas.

A implementação abrange desde testes de segurança automatizados (SAST, DAST, SCA) até controles de compliance e monitoramento contínuo de vulnerabilidades.

## 🎯 Objetivos de Cybersecurity
- **SSDLC Implementation**: Integração de práticas de desenvolvimento seguro
- **Automated Security Testing**: SAST, DAST e SCA no pipeline CI/CD
- **LGPD Compliance**: Controles automatizados de proteção de dados
- **Vulnerability Management**: Gestão contínua de vulnerabilidades
- **Security Monitoring**: Dashboard e alertas em tempo real
- **DevSecOps Integration**: Segurança integrada ao ciclo de desenvolvimento

## 🛡️ Tecnologias de Segurança Utilizadas

### Core Security Stack
- **Spring Security 6.x** - Framework de segurança
- **JWT (JSON Web Token)** - Autenticação stateless
- **BCrypt** - Hash de senhas
- **OWASP ZAP** - Dynamic Application Security Testing (DAST)
- **SonarQube** - Static Application Security Testing (SAST)
- **Snyk** - Software Composition Analysis (SCA)
- **OWASP Dependency Check** - Análise de dependências

### Security Tools & Frameworks
- **SpotBugs** - Análise estática de código
- **PMD** - Detecção de vulnerabilidades
- **Semgrep** - SAST avançado
- **Nikto** - Web vulnerability scanner
- **JaCoCo** - Code coverage para testes de segurança

## 🔐 Funcionalidades de Segurança Implementadas

### Autenticação e Autorização
- ✅ JWT Authentication com refresh tokens
- ✅ Role-based Access Control (RBAC)
- ✅ Session management seguro
- ✅ Password policy enforcement
- ✅ Account lockout protection

### Proteções de Aplicação
- ✅ Input validation e sanitização
- ✅ SQL Injection prevention
- ✅ XSS protection headers
- ✅ CSRF protection
- ✅ Rate limiting
- ✅ Security headers (HSTS, CSP, etc.)

### LGPD Compliance
- ✅ Data minimization controls
- ✅ Consent management
- ✅ Data subject rights (access, deletion, portability)
- ✅ Audit logging
- ✅ Data encryption at rest and in transit
- ✅ Privacy by design implementation

### Vulnerability Management
- ✅ Continuous vulnerability scanning
- ✅ CVSS-based risk prioritization
- ✅ Automated remediation workflows
- ✅ Security metrics and KPIs
- ✅ Real-time alerting system

## 🏗️ Arquitetura de Segurança

### Estrutura do Projeto de Security
```
src/main/java/com/example/betaware/
├── security/                    # 🔐 Configurações de segurança
│   ├── SecurityConfig.java      # Configuração principal de segurança
│   ├── JwtTokenProvider.java    # Provedor de tokens JWT
│   └── JwtAuthenticationFilter.java # Filtro de autenticação
├── service/                     # 🛡️ Services de segurança
│   ├── VulnerabilityManagementService.java # Gestão de vulnerabilidades
│   └── LGPDComplianceService.java # Compliance LGPD
├── controller/                  # 📊 Controllers de segurança
│   └── SecurityDashboardController.java # Dashboard de segurança
└── test/                       # 🧪 Testes de segurança
    └── SecurityValidationTest.java # Testes automatizados
```

### Security Pipeline Integration
```
.github/workflows/
├── security-pipeline.yml       # Pipeline principal de segurança
└── security-monitoring.yml     # Monitoramento contínuo

scripts/
├── security-scan.sh           # Scripts de scan (Linux/Mac)
└── security-scan.bat          # Scripts de scan (Windows)

security-policies.yml          # Políticas de segurança
dependency-check-suppressions.xml # Supressões de falsos positivos
spotbugs-security-include.xml  # Regras específicas do SpotBugs
```

## 🔍 Componentes de Segurança Implementados

### 1. SecurityConfig.java
**Localização**: `src/main/java/com/example/betaware/security/SecurityConfig.java`

**Funcionalidades**:
- Configuração JWT completa com validação de tokens
- Security headers avançados (HSTS, CSP, X-Frame-Options)
- CORS configuration para múltiplos domínios
- Session management stateless
- Password encoding com BCrypt
- Event logging para auditoria LGPD

### 2. VulnerabilityManagementService.java
**Localização**: `src/main/java/com/example/betaware/service/VulnerabilityManagementService.java`

**Funcionalidades**:
- Integração com Snyk, SonarQube e OWASP ZAP
- Priorização automática baseada em CVSS
- Workflow de remediação automatizada
- Métricas e relatórios de vulnerabilidades
- Alertas críticos em tempo real

### 3. LGPDComplianceService.java
**Localização**: `src/main/java/com/example/betaware/service/LGPDComplianceService.java`

**Funcionalidades**:
- Gestão de consentimento automatizada
- Controles de minimização de dados
- Implementação de direitos do titular
- Audit trail completo
- Criptografia de dados sensíveis

### 4. SecurityDashboardController.java
**Localização**: `src/main/java/com/example/betaware/controller/SecurityDashboardController.java`

**Funcionalidades**:
- Dashboard em tempo real de métricas de segurança
- Relatórios de compliance LGPD
- Alertas e notificações de segurança
- APIs para integração com ferramentas externas

### 5. SecurityValidationTest.java
**Localização**: `src/test/java/com/example/betaware/security/SecurityValidationTest.java`

**Funcionalidades**:
- Testes automatizados de input validation
- Verificação de autenticação e autorização
- Testes de security headers
- Validação de compliance LGPD
- Testes de rate limiting

## 🔧 Endpoints de Segurança

### Security Dashboard
- GET `/api/v1/security/dashboard/metrics` - Métricas de segurança em tempo real
- GET `/api/v1/security/dashboard/vulnerabilities` - Status de vulnerabilidades
- GET `/api/v1/security/dashboard/alerts` - Alertas críticos de segurança
- GET `/api/v1/security/dashboard/compliance` - Status de compliance LGPD

### LGPD Compliance
- POST `/api/v1/security/lgpd/consent` - Registrar consentimento
- GET `/api/v1/security/lgpd/data/{userId}` - Exportar dados do usuário
- DELETE `/api/v1/security/lgpd/data/{userId}` - Solicitar exclusão de dados
- GET `/api/v1/security/lgpd/audit` - Relatório de auditoria

### Vulnerability Management
- GET `/api/v1/security/vulnerabilities` - Listar vulnerabilidades
- POST `/api/v1/security/vulnerabilities/scan` - Iniciar scan de segurança
- PUT `/api/v1/security/vulnerabilities/{id}/remediate` - Remediar vulnerabilidade

## 🚀 Configuração e Execução

### Pré-requisitos de Segurança
- Java 17 ou superior
- Maven 3.8+
- Docker (para ferramentas de segurança)
- OWASP ZAP (para DAST)
- SonarQube (para SAST)

### Configuração das Ferramentas de Segurança

#### 1. OWASP ZAP (DAST)
```bash
# Instalar OWASP ZAP
docker pull owasp/zap2docker-stable

# Executar scan DAST
./scripts/security-scan.sh dast
```

#### 2. SonarQube (SAST)
```bash
# Executar análise SonarQube
mvn sonar:sonar -Dsonar.projectKey=betaware-api
```

#### 3. Dependency Check (SCA)
```bash
# Executar verificação de dependências
mvn org.owasp:dependency-check-maven:check
```

### Executando o Projeto com Segurança
```bash
# Clone o repositório
git clone https://github.com/pedrobicas/BetAwareAPI.git

# Entre no diretório
cd BetAwareAPI

# Execute todos os testes de segurança
./scripts/security-scan.sh all

# Compile com verificações de segurança
mvn clean install -Psecurity

# Execute a aplicação
mvn spring-boot:run
```

## 🧪 Executando Testes de Segurança

### Testes Automatizados
```bash
# Executar todos os testes de segurança
mvn test -Dtest=SecurityValidationTest

# Executar testes específicos
mvn test -Dtest=SecurityValidationTest#testInputValidation
mvn test -Dtest=SecurityValidationTest#testJWTAuthentication
mvn test -Dtest=SecurityValidationTest#testLGPDCompliance
```

### Pipeline de Segurança CI/CD
```bash
# Executar pipeline completo de segurança
./.github/workflows/security-pipeline.yml

# Monitoramento contínuo
./.github/workflows/security-monitoring.yml
```

## 📊 Métricas e Monitoramento

### Dashboard de Segurança
Acesse o dashboard de segurança em tempo real:
- URL: `http://localhost:8080/api/v1/security/dashboard`
- Métricas: Vulnerabilidades, Compliance, Performance
- Alertas: Críticos, Médios, Baixos

### Relatórios de Compliance
- **LGPD Compliance Report**: Status detalhado de conformidade
- **Vulnerability Assessment**: Análise de riscos e remediação
- **Security Audit Trail**: Log completo de eventos de segurança

## 📋 Sprints de Cybersecurity Implementados

### Sprint 3: Automated Security Testing
- ✅ **SAST Integration**: SonarQube, SpotBugs, PMD, Semgrep
- ✅ **DAST Implementation**: OWASP ZAP, Nikto
- ✅ **SCA Analysis**: OWASP Dependency Check, Snyk
- ✅ **CI/CD Integration**: Unified security pipeline

### Sprint 4: SSDLC & Compliance
- ✅ **SSDLC Implementation**: Secure coding practices
- ✅ **Vulnerability Management**: Continuous monitoring
- ✅ **LGPD Compliance**: Automated controls
- ✅ **Security Testing**: Comprehensive validation


## 📚 Documentação de Segurança

### Relatório Completo de Implementação
Para documentação detalhada de toda a implementação de segurança, consulte:
- **[SECURITY_IMPLEMENTATION_REPORT.md](SECURITY_IMPLEMENTATION_REPORT.md)** - Relatório completo com arquitetura, métricas, procedimentos operacionais e recomendações futuras

### Documentação da API de Segurança
A documentação completa dos endpoints de segurança está disponível através do Swagger UI:
- URL: `http://localhost:8080/api/swagger-ui.html`
- Seção: **Security Dashboard Controller**

### Políticas e Configurações
- **[security-policies.yml](security-policies.yml)** - Políticas de segurança do projeto
- **[dependency-check-suppressions.xml](dependency-check-suppressions.xml)** - Configurações de supressão
- **[spotbugs-security-include.xml](spotbugs-security-include.xml)** - Regras de segurança SpotBugs

## 🎓 Contexto Acadêmico - Cybersecurity

### Disciplina: Cybersecurity
**Objetivo**: Implementar uma solução completa de segurança em aplicações Spring Boot, demonstrando:

1. **SSDLC (Secure Software Development Lifecycle)**
   - Integração de segurança no ciclo de desenvolvimento
   - Práticas de código seguro
   - Testes automatizados de segurança

2. **DevSecOps Implementation**
   - Pipeline CI/CD com segurança integrada
   - Monitoramento contínuo de vulnerabilidades
   - Automação de controles de segurança

3. **LGPD Compliance**
   - Implementação de controles de proteção de dados
   - Gestão de consentimento e direitos do titular
   - Auditoria e relatórios de compliance

4. **Security Testing Automation**
   - SAST (Static Application Security Testing)
   - DAST (Dynamic Application Security Testing)
   - SCA (Software Composition Analysis)

### Resultados Alcançados
- ✅ **100% de automação** nos testes de segurança
- ✅ **Compliance total** com LGPD
- ✅ **Monitoramento 24/7** de vulnerabilidades
- ✅ **Dashboard em tempo real** de métricas de segurança
- ✅ **Pipeline DevSecOps** completamente integrado

## 👥 Equipe de Desenvolvimento - Cybersecurity
- **Felipe Terra** – RM 99405 - Security Architecture & SSDLC
- **Pedro Bicas** – RM 99534 - LGPD Compliance & Vulnerability Management  
- **Gabriel Doms** – RM 98630 - Security Testing & Automation
- **Lucas Vassão** – RM 98607 - DevSecOps & CI/CD Integration
- **Bryan Willian** – RM 551305 - Security Monitoring & Dashboard

## 🔐 Certificações e Compliance

### Standards Implementados
- **OWASP Top 10** - Proteção contra as principais vulnerabilidades web
- **LGPD (Lei Geral de Proteção de Dados)** - Compliance total com a legislação brasileira
- **ISO 27001** - Controles de segurança da informação
- **NIST Cybersecurity Framework** - Framework de segurança cibernética

### Security Controls
- **Authentication & Authorization** - JWT com RBAC
- **Data Protection** - Criptografia e minimização de dados
- **Vulnerability Management** - Scan contínuo e remediação automatizada
- **Incident Response** - Alertas e procedimentos automatizados
- **Audit & Compliance** - Logs detalhados e relatórios

## 🚀 Próximos Passos e Melhorias

### Implementações Futuras Recomendadas
1. **WAF (Web Application Firewall)** - Proteção adicional contra ataques
2. **SIEM Integration** - Correlação de eventos de segurança
3. **Threat Intelligence** - Integração com feeds de ameaças
4. **Zero Trust Architecture** - Implementação de arquitetura zero trust
5. **Container Security** - Segurança para ambientes containerizados

### Monitoramento Contínuo
- Scans automatizados diários de vulnerabilidades
- Alertas em tempo real para incidentes críticos
- Relatórios semanais de compliance LGPD
- Métricas mensais de postura de segurança

## 📞 Suporte e Contato

Para questões relacionadas à implementação de segurança:
- **Security Team**: security@betaware.com
- **LGPD Compliance**: lgpd@betaware.com
- **Incident Response**: incident@betaware.com

---

**⚠️ Nota Importante**: Este projeto foi desenvolvido exclusivamente para fins acadêmicos na disciplina de Cybersecurity. Todas as implementações seguem as melhores práticas de segurança e compliance com LGPD.
