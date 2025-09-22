# 🔐 Configuração de Secrets - GitHub Actions

## Secrets Obrigatórios

### 1. SONAR_TOKEN (SonarQube Cloud)
```
Nome: SONAR_TOKEN
Valor: Seu token do SonarCloud
```

**Como obter:**
1. Acesse: https://sonarcloud.io/
2. Faça login com GitHub
3. Vá em: My Account > Security > Generate Tokens
4. Nome: "BetAware DevSecOps"
5. Copie o token gerado

### 2. SNYK_TOKEN (Snyk)
```
Nome: SNYK_TOKEN
Valor: Seu token da Snyk
```

**Como obter:**
1. Acesse: https://snyk.io/
2. Crie conta gratuita
3. Vá em: Settings > General > Auth Token
4. Copie o token

## Secrets Opcionais (Recomendados)

### 3. SLACK_WEBHOOK_URL (Notificações)
```
Nome: SLACK_WEBHOOK_URL
Valor: https://hooks.slack.com/services/...
```

**Como obter:**
1. Acesse workspace do Slack
2. Apps > Incoming Webhooks
3. Configure para um canal (#devsecops)
4. Copie a webhook URL

### 4. SEMGREP_APP_TOKEN (Semgrep Registry)
```
Nome: SEMGREP_APP_TOKEN
Valor: Seu token do Semgrep
```

**Como obter:**
1. Acesse: https://semgrep.dev/
2. Crie conta gratuita
3. Settings > Tokens
4. Generate new token

## ✅ Verificação
Após configurar, você deve ter pelo menos:
- ✅ SONAR_TOKEN
- ✅ SNYK_TOKEN

Os outros são opcionais mas recomendados para experiência completa.