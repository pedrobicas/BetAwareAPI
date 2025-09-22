# Documentação do Pipeline CI/CD de Segurança - BetAware API

## Visão Geral

Este documento fornece instruções detalhadas para configuração, execução e manutenção do pipeline de segurança CI/CD implementado para o projeto BetAware API. O pipeline integra testes automatizados de SAST, DAST e SCA com políticas de quality gates e notificações em tempo real.

## Arquitetura do Pipeline

### Diagrama de Fluxo
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Developer     │───▶│  GitHub Actions  │───▶│  Quality Gates  │
│   Push/PR       │    │   CI/CD Pipeline │    │   & Deployment  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
            ┌───────▼──┐  ┌────▼────┐  ┌──▼─────┐
            │   SAST   │  │   SCA   │  │  DAST  │
            │ Analysis │  │ Analysis│  │Analysis│
            └──────────┘  └─────────┘  └────────┘
                    │           │           │
                    └───────────┼───────────┘
                                │
                        ┌───────▼─────────┐
                        │ Security Reports│
                        │ & Notifications │
                        └─────────────────┘
```

### Componentes do Pipeline

1. **Triggers**: Push, Pull Request, Schedule
2. **Security Analysis**: SAST, SCA, DAST
3. **Quality Gates**: Automated decision making
4. **Reporting**: Consolidated security reports
5. **Notifications**: Team alerts and dashboard updates

## Configuração Inicial

### 1. Pré-requisitos

#### Ferramentas Necessárias
```bash
# Verificar versões
java -version      # Java 17+
mvn -version       # Maven 3.8+
git --version      # Git 2.x+
docker --version   # Docker 20.x+ (para DAST)
```

#### Tokens e Secrets Necessários
```yaml
# GitHub Secrets (Settings > Secrets and variables > Actions)
SONAR_TOKEN:           # Token do SonarCloud
SONAR_ORGANIZATION:    # Nome da organização SonarCloud
SNYK_TOKEN:           # Token da conta Snyk
SEMGREP_APP_TOKEN:    # Token Semgrep (opcional)
```

### 2. Setup do SonarCloud

#### Passo 1: Criar Projeto no SonarCloud
1. Acesse https://sonarcloud.io
2. Conecte com GitHub
3. Import project `betaware-api`
4. Configure quality gate personalizado

#### Passo 2: Configurar Quality Gate
```yaml
# Critérios recomendados
Maintainability Rating: A
Reliability Rating: A
Security Rating: A
Security Hotspots Reviewed: 100%
Coverage: > 80%
Duplicated Lines: < 3%
```

#### Passo 3: Adicionar Token
```bash
# Gerar token em SonarCloud > My Account > Security
# Adicionar como SONAR_TOKEN nos GitHub Secrets
```

### 3. Setup do Snyk

#### Passo 1: Criar Conta Snyk
1. Acesse https://snyk.io
2. Conecte com GitHub
3. Import project BetAware API

#### Passo 2: Configurar Integração
```bash
# Instalar Snyk CLI
npm install -g snyk

# Autenticar
snyk auth

# Testar projeto
snyk test --file=pom.xml
```

#### Passo 3: Adicionar Token
```bash
# Obter token em Snyk > Settings > API Token
# Adicionar como SNYK_TOKEN nos GitHub Secrets
```

### 4. Setup do Semgrep (Opcional)

#### Passo 1: Criar Conta Semgrep
1. Acesse https://semgrep.dev
2. Conecte com GitHub
3. Configure regras personalizadas

#### Passo 2: Configurar Rules
```yaml
# .semgrep/settings.yml já configurado
# Personalizar conforme necessário
```

## Execução do Pipeline

### 1. Triggers Automáticos

#### Push para Branch Principal
```yaml
on:
  push:
    branches: [ main, master, develop ]
```
**Executa**: SAST + SCA + Build + DAST + Deploy

#### Pull Request
```yaml
on:
  pull_request:
    branches: [ main, master ]
```
**Executa**: SAST + SCA + Build (sem DAST/Deploy)

#### Schedule Diário
```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC
```
**Executa**: Full security scan + dependency check

### 2. Execução Manual

#### Via GitHub Actions UI
1. Acesse `Actions` tab no GitHub
2. Selecione `Security CI/CD Pipeline`
3. Click `Run workflow`
4. Selecione branch e execute

#### Via GitHub CLI
```bash
# Instalar GitHub CLI
gh auth login

# Executar workflow
gh workflow run "Security CI/CD Pipeline" \
  --ref main \
  --field environment=staging
```

### 3. Execução Local

#### SAST Local
```bash
# Semgrep
docker run --rm -v "${PWD}:/src" \
  returntocorp/semgrep \
  semgrep --config=auto --json --output=semgrep-results.json /src

# SpotBugs + FindSecBugs
mvn clean compile spotbugs:check

# PMD
mvn pmd:check

# SonarQube (requer SonarQube local)
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin
```

#### SCA Local
```bash
# OWASP Dependency Check
mvn dependency-check:check

# Snyk
snyk test --file=pom.xml --severity-threshold=high
```

#### DAST Local
```bash
# Iniciar aplicação
mvn spring-boot:run &
sleep 30

# OWASP ZAP Baseline Scan
docker run -v $(pwd)/.zap:/zap/wrk/:rw \
  -t owasp/zap2docker-stable \
  zap-baseline.py \
  -t http://host.docker.internal:8080 \
  -r zap-report.html \
  -x zap-report.xml

# Parar aplicação
pkill -f "spring-boot:run"
```

## Quality Gates e Políticas

### 1. Critérios de Bloqueio (Pipeline Falha)

#### SAST
```yaml
Critérios de Falha:
- Vulnerabilidades Críticas > 0
- Security Hotspots não revisados > 5
- Coverage < 70%
- Duplicated code > 5%
```

#### SCA
```yaml
Critérios de Falha:
- CVEs Críticos (CVSS >= 9.0) > 0
- CVEs Altos com exploit público > 0
- Licenças proibidas detectadas
- Dependências abandonadas (> 2 anos) > 0
```

#### DAST
```yaml
Critérios de Falha:
- Vulnerabilidades Críticas > 0
- Vulnerabilidades Altas > 2
- Rate limiting não implementado
- Security headers ausentes > 3
```

### 2. Configuração de Quality Gates

#### GitHub Branch Protection
```yaml
# .github/branch-protection.yml
protection_rules:
  main:
    required_status_checks:
      strict: true
      contexts:
        - "SAST - Static Security Analysis"
        - "SCA - Dependency Security Analysis"
        - "Build and Unit Tests"
    enforce_admins: true
    required_pull_request_reviews:
      required_approving_review_count: 2
      dismiss_stale_reviews: true
      require_code_owner_reviews: true
```

#### Automated Policies
```yaml
# Auto-merge de patches de segurança
- name: Auto-merge security patches
  if: |
    github.event.pull_request.user.login == 'dependabot[bot]' &&
    contains(github.event.pull_request.labels.*.name, 'security')
  run: gh pr merge --auto --merge
```

## Relatórios e Dashboards

### 1. Tipos de Relatórios Gerados

#### Imediatos (Por Build)
- **SAST Report**: `target/spotbugs.xml`, `semgrep.sarif`
- **SCA Report**: `target/dependency-check/*`
- **DAST Report**: `zap-report.html`, `zap-report.xml`
- **Summary**: `security-reports/SECURITY_SUMMARY.md`

#### Consolidados (Semanais)
- **Executive Summary**: Métricas de alto nível
- **Trend Analysis**: Evolução da postura de segurança
- **Compliance Report**: Aderência às políticas

#### Dashboards em Tempo Real
- **SonarCloud**: https://sonarcloud.io/project/betaware-api
- **Snyk**: Dashboard de vulnerabilidades
- **GitHub Security**: Alerts e SARIF reports

### 2. Configuração de Notificações

#### Slack Integration
```yaml
# .github/workflows/notify-slack.yml
- name: Notify Slack on Failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: failure
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
    channel: '#security-alerts'
    message: |
      🚨 Security Pipeline Failed!
      
      Repository: ${{ github.repository }}
      Branch: ${{ github.ref }}
      Commit: ${{ github.sha }}
      
      Check details: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

#### Email Notifications
```yaml
# .github/workflows/notify-email.yml
- name: Send Email on Critical Findings
  if: contains(steps.security-scan.outputs.result, 'CRITICAL')
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.company.com
    server_port: 587
    username: ${{ secrets.SMTP_USERNAME }}
    password: ${{ secrets.SMTP_PASSWORD }}
    subject: "🚨 Critical Security Vulnerabilities Detected"
    to: security-team@company.com
    from: noreply@company.com
    body: |
      Critical security vulnerabilities have been detected in BetAware API.
      
      Please review the security reports immediately.
```

#### Microsoft Teams Integration
```yaml
- name: Notify Teams
  uses: jdcargile/ms-teams-notification@v1.3
  with:
    github-token: ${{ github.token }}
    ms-teams-webhook-uri: ${{ secrets.MS_TEAMS_WEBHOOK }}
    notification-summary: Security scan completed
    notification-color: ${{ job.status == 'success' && '28a745' || 'dc3545' }}
```

## Exemplos de Execução

### 1. Pipeline de Sucesso

#### Log de Exemplo
```
🟢 Security CI/CD Pipeline - SUCCESS
📅 2024-01-15 14:30:00 UTC
🔄 Triggered by: push to main
👤 Author: developer@company.com

📊 RESULTS SUMMARY:
✅ SAST Analysis: PASSED
   - Semgrep: 0 critical, 2 medium issues
   - SonarQube: Grade A, 89% coverage
   - SpotBugs: 0 security bugs
   - PMD: 1 minor violation

✅ SCA Analysis: PASSED
   - OWASP Dependency Check: 0 high/critical CVEs
   - Snyk: 3 low-severity issues
   - License compliance: OK

✅ Build & Tests: PASSED
   - Unit tests: 45/45 passed
   - Integration tests: 12/12 passed
   - Coverage: 89.3%

✅ DAST Analysis: PASSED
   - OWASP ZAP: 0 high-risk vulnerabilities
   - API Security Tests: All passed
   - Security headers: 8/10 implemented

🚀 DEPLOYMENT: Approved for staging
📋 Reports: Available in artifacts
```

### 2. Pipeline com Falhas

#### Log de Exemplo
```
🔴 Security CI/CD Pipeline - FAILED
📅 2024-01-15 16:45:00 UTC
🔄 Triggered by: push to feature/new-endpoint
👤 Author: developer@company.com

📊 FAILURE SUMMARY:
❌ SAST Analysis: FAILED
   - Semgrep: 1 CRITICAL issue (hardcoded secret)
   - SonarQube: Grade C, security hotspots
   
❌ SCA Analysis: FAILED
   - OWASP Dependency Check: 1 HIGH CVE detected
   - Affected: jackson-databind < 2.15.4
   
⚠️ Build & Tests: PASSED (with warnings)
   - Unit tests: 44/45 passed (1 skipped)
   - Coverage: 76.2% (below threshold)

❌ DAST Analysis: BLOCKED
   - Cannot proceed due to SAST/SCA failures

🚫 DEPLOYMENT: BLOCKED
📋 Action Required: Fix critical issues before merge

🔧 RECOMMENDED ACTIONS:
1. Fix hardcoded JWT secret in application.properties
2. Update jackson-databind to 2.15.4
3. Increase test coverage above 80%
4. Address security hotspots in SonarQube
```

### 3. Pipeline de Pull Request

#### Log de Exemplo
```
🟡 Security CI/CD Pipeline - PR CHECK
📅 2024-01-15 11:20:00 UTC
🔄 Triggered by: pull_request #123
👤 Author: developer@company.com
🎯 Target: main <- feature/api-improvements

📊 PR SECURITY ANALYSIS:
✅ SAST Analysis: PASSED
   - No new security issues introduced
   - Code quality maintained
   
✅ SCA Analysis: PASSED
   - No new vulnerable dependencies
   - 1 dependency updated (patch version)
   
✅ Build & Tests: PASSED
   - All tests passing
   - Coverage: 87.5% (+2.3%)

ℹ️ DAST Analysis: SKIPPED
   - Runs only on main branch

✅ PR APPROVAL: Ready for review
📝 Changes Summary:
   - 3 files modified
   - 0 security issues
   - Coverage improved
   
🔍 Security Review: Not required (low risk changes)
```

## Troubleshooting

### 1. Problemas Comuns

#### Pipeline Failing no Setup
```bash
# Verificar secrets
gh secret list

# Verificar conexão SonarCloud
curl -u $SONAR_TOKEN: https://sonarcloud.io/api/user_tokens/search

# Verificar Snyk
snyk auth
snyk test --dry-run
```

#### Timeout em DAST
```yaml
# Aumentar timeout no workflow
- name: OWASP ZAP Baseline Scan
  timeout-minutes: 30  # Aumentar de 15 para 30
```

#### Falsos Positivos
```xml
<!-- Adicionar supressões -->
<!-- spotbugs-security-include.xml -->
<exclude>
    <Class name="com.example.TestClass"/>
    <Bug pattern="HARDCODED_PASSWORD"/>
</exclude>
```

### 2. Debug do Pipeline

#### Habilitar Debug Logs
```yaml
# No workflow
env:
  ACTIONS_STEP_DEBUG: true
  ACTIONS_RUNNER_DEBUG: true
```

#### Logs Detalhados por Ferramenta
```bash
# Semgrep
semgrep --verbose --config=auto

# Maven com debug
mvn -X dependency-check:check

# ZAP com logs
docker run ... -z "-silent -addonupdate"
```

### 3. Performance Optimization

#### Cache de Dependências
```yaml
- name: Cache Maven dependencies
  uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
```

#### Execução Paralela
```yaml
strategy:
  matrix:
    analysis: [sast, sca]
  max-parallel: 2
```

## Manutenção

### 1. Atualizações Regulares

#### Mensal
- [ ] Atualizar versões das ferramentas de segurança
- [ ] Revisar e ajustar quality gates
- [ ] Análise de métricas de pipeline

#### Trimestral
- [ ] Review completo das regras SAST
- [ ] Atualização de supressões obsoletas
- [ ] Análise de tendências de segurança

#### Anual
- [ ] Avaliação completa do pipeline
- [ ] Integração de novas ferramentas
- [ ] Training da equipe

### 2. Monitoramento da Saúde do Pipeline

#### Métricas-Chave
```yaml
Pipeline Health Metrics:
- Success Rate: > 95%
- Average Duration: < 15 min
- False Positive Rate: < 5%
- MTTR (Mean Time to Repair): < 4 hours
```

#### Alertas Automáticos
```yaml
# Pipeline health monitoring
- name: Check Pipeline Health
  if: always()
  run: |
    FAILURE_RATE=$(gh api repos/${{ github.repository }}/actions/workflows/security-pipeline.yml/runs --jq '.workflow_runs | map(select(.conclusion == "failure")) | length')
    if [ "$FAILURE_RATE" -gt 5 ]; then
      echo "High failure rate detected"
      # Send alert to DevOps team
    fi
```

### 3. Evolução do Pipeline

#### Roadmap de Melhorias
1. **Q1 2024**: Integration testing security
2. **Q2 2024**: Container security scanning
3. **Q3 2024**: Infrastructure as Code security
4. **Q4 2024**: Runtime security monitoring

#### Feedback Loop
```yaml
# Collect metrics for improvement
- name: Collect Pipeline Metrics
  run: |
    echo "SAST_DURATION=${{ steps.sast.outputs.duration }}" >> pipeline-metrics.json
    echo "SCA_DURATION=${{ steps.sca.outputs.duration }}" >> pipeline-metrics.json
    echo "DAST_DURATION=${{ steps.dast.outputs.duration }}" >> pipeline-metrics.json
```

## Compliance e Auditoria

### 1. Evidence Collection

#### Logs de Auditoria
```bash
# Todos os logs são automaticamente coletados
- Build logs
- Security scan results
- Quality gate decisions
- Deployment approvals
```

#### Retenção de Dados
```yaml
Retention Policy:
- Security reports: 2 years
- Pipeline logs: 1 year  
- Artifacts: 90 days
- Metrics: 5 years
```

### 2. Relatórios de Compliance

#### Geração Automática
```bash
# Weekly compliance report
gh workflow run compliance-report.yml
```

#### Métricas SOC 2
- Pipeline execution logs
- Access control evidence
- Change management records
- Incident response logs

---

**Documentação mantida por**: Security & DevOps Team  
**Última atualização**: $(date)  
**Versão**: 2.0  
**Próxima revisão**: Trimestral