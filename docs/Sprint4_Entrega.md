# Sprint 4 — Entrega e Guia Explicativo (BetAware API)

Este guia resume como a API atende aos requisitos de SOA na Sprint 4 e oferece um roteiro de vídeo para apresentar a estrutura, princípios SOLID, segurança stateless com JWT e regras de negócio por services.

## Visão Geral
- Arquitetura em camadas: `controller` → `service` → `repository` → `model`.
- Segurança stateless com JWT, filtro de autenticação e `BCryptPasswordEncoder`.
- Boas práticas: DTOs, validação, tratamento de exceções global, CORS configurado.
- Integração com Swagger/OpenAPI e health check.

## Mapeamento dos Requisitos
- Interfaces e polimorfismo:
  - `UserDetailsService` implementado por `CustomUserDetailsService`.
  - `Repository` interfaces (JPA) para `ApostaRepository` e `UsuarioRepository`.
  - `PasswordEncoder` injetado como interface (implementação `BCryptPasswordEncoder`).
- Despacho dinâmico de métodos: injeção de dependências por interface (ex.: `UserDetailsService`, `PasswordEncoder`).
- Separação de responsabilidades:
  - `Controller`: `ApostaController`, `AuthController`, `HealthController`.
  - `Service`: `ApostaService`, `AuthService` encapsulam regras de negócio.
  - `Repository`: `ApostaRepository`, `UsuarioRepository`.
- SOLID:
  - SRP: controllers focados em I/O; services focados em regras; repositórios focados em dados.
  - OCP: novos filtros/endpoints podem ser adicionados sem modificar existentes.
  - LSP/ISP/DIP: uso de interfaces do Spring (`UserDetailsService`, `PasswordEncoder`, `JpaRepository`).
- Segurança:
  - Stateless com `SessionCreationPolicy.STATELESS`.
  - Filtro `JwtAuthenticationFilter` (`OncePerRequestFilter`) intercepta `Authorization` e autentica via token.
  - Tokens gerados/validados por `JwtTokenProvider` com chave HMAC.
  - Senhas codificadas com `BCryptPasswordEncoder`.
- Regras de negócio:
  - `ApostaService` implementa criação e consultas por usuário/período.
  - DTOs (`ApostaDTO`) traduzem entidades para respostas estáveis.

## Como executar
- Context path: todas as rotas iniciam com `/api`.
- Rodar local (Windows):
  - `mvnw.cmd clean install`
  - `mvnw.cmd spring-boot:run`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- H2 Console: `http://localhost:8080/api/h2-console`

## Roteiro de Vídeo (7–10 minutos)
1. Introdução (30s)
   - Objetivo da Sprint e foco em SOA, SOLID, segurança.
2. Estrutura do projeto (1–2min)
   - Mostrar pastas: `controller`, `service`, `repository`, `model`, `security`, `config`, `exception`.
   - Explicar separação de responsabilidades e uso de DTOs.
3. Segurança (2–3min)
   - `SecurityFilterChain` com `STATELESS` e `JwtAuthenticationFilter` antes do `UsernamePasswordAuthenticationFilter`.
   - `JwtTokenProvider`: geração (subject, expiração) e validação.
   - `BCryptPasswordEncoder` no bean e uso em `AuthService`.
   - Swagger protegido com Bearer.
4. Interfaces, polimorfismo e DIP (1–2min)
   - Injeção via `UserDetailsService` e `PasswordEncoder`.
   - Repositórios como interfaces JPA.
5. Regras de negócio em services (1–2min)
   - `ApostaService`: métodos de criação e filtros por período/usuário.
   - Conversão para `ApostaDTO`.
6. Demonstração dos endpoints (2–3min)
   - `POST /api/v1/auth/register` e `POST /api/v1/auth/login`.
   - Usar JWT no `Authorize` do Swagger.
   - `POST /api/v1/apostas` e `GET /api/v1/apostas`.
   - Filtros: `GET /api/v1/apostas/periodo` e `GET /api/v1/apostas/usuario/periodo`.
7. Encerramento (30s)
   - Principais aprendizados, extensibilidade futura (ex.: relatórios, auditoria).

## Dicas de apresentação
- Deixe o logger de segurança em `DEBUG` para visualizar o filtro funcionando.
- Demonstre erro com token inválido e o tratamento pelo `GlobalExceptionHandler`.
- Mostre as anotações de validação nas entidades e DTOs.

## Integração Auth0 (Opcional)
- Alternativa com `spring-boot-starter-oauth2-resource-server`:
  - Configurar `spring.security.oauth2.resourceserver.jwt.issuer-uri` e `audiences`.
  - Ajustar `SecurityFilterChain` para `http.oauth2ResourceServer().jwt()`.
  - Manter serviços/controllers inalterados; apenas a origem do token muda.

---
Este guia acompanha o código fonte para a entrega da Sprint 4, garantindo clareza na arquitetura, segurança e práticas de desenvolvimento.