# Event Reservations - Backend

Sistema de reserva de vagas para eventos com suporte a alta concorrência e notificações por email.

## Tecnologias

- **Java 17** + **Spring Boot 3.2.5**
- **H2 Database** (em memória para testes, facilmente migrável para PostgreSQL/MySQL)
- **Flyway** (versionamento de schema)
- **Spring Data JPA** + **Hibernate**
- **Spring Mail** (envio de emails via SMTP)
- **JUnit 5** + **Mockito** (testes unitários)

## Requisitos

- Java 17 ou superior
- Maven 3.8+
- Docker (opcional, para MailHog em desenvolvimento)

## Instalação e Execução

### 1. Compilar e Rodar Localmente

```bash
cd projeto-paripassu-backend

# Build
./mvnw clean install

# Executar aplicação
./mvnw spring-boot:run
```

Backend estará disponível em: `http://localhost:8081`

### 2. Configuração de Email (SMTP)

Por padrão, a aplicação tenta conectar a um servidor SMTP em `localhost:1025` (MailHog).

Para usar um servidor real (ex: Brevo, Gmail), defina as variáveis:

```bash
export MAIL_HOST=smtp-relay.brevo.com
export MAIL_PORT=587
export MAIL_USERNAME=seu-usuario
export MAIL_PASSWORD=sua-senha
export MAIL_SMTP_AUTH=true
export MAIL_SMTP_STARTTLS=true
export MAIL_FROM=seu-email@dominio.com

./mvnw spring-boot:run
```

### 3. Rodar Testes

```bash
# Todos os testes
./mvnw test

# Teste específico
./mvnw -Dtest=EmailNotifierTest test
```

### 4. H2 Console (Development)

Acesse: `http://localhost:8081/h2-console`
- URL: `jdbc:h2:mem:reservationsdb`
- Usuário: `sa`
- Senha: (deixe em branco)

## Decisões de Arquitetura

### 1. Controle de Concorrência com Pessimistic Locking

**Problema**: Duas requisições simultâneas podem decrementar `availableSlots` para valores inválidos (ex: duas pessoas ganham "a última vaga").

**Solução**: 
- Usar `@Lock(LockModeType.PESSIMISTIC_WRITE)` no repositório `Event`
- Cada thread aguarda sua vez para decrementar o contador atomicamente
- Evita race conditions sem overhead de retry

**Código**:
```java
@Query("SELECT e FROM Event e WHERE e.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Event> findByIdWithLock(Long id);
```

### 2. Idempotência com `Idempotency-Key`

**Problema**: Cliente pode reenviar a mesma requisição (conexão instável) e criar duplicatas.

**Solução**:
- Cliente envia header `Idempotency-Key` (UUID único por requisição)
- Banco de dados valida constraint `UNIQUE (event_id, email)` para evitar duplicatas
- A resposta é sempre `201 CREATED` mesmo em reenvios

**Código**:
```java
if (registrationRepository.existsByEventIdAndEmail(event.getId(), requestDTO.getEmail())) {
    throw new DuplicateEmailException("Email já registrado para este evento");
}
```

### 3. Notification Outbox Pattern

**Problema**: Email pode ser enviado múltiplas vezes ou nunca ser enviado se o sistema falhar.

**Solução**:
- Ao registrar participante, salvar entrada em tabela `notification_outbox` com status `PENDING`
- `NotificationOutboxProcessor` (agendado a cada 5 segundos) processa mensagens pendentes
- Após envio bem-sucedido, marca como `SENT`
- Em caso de erro, incrementa `attempts` e marca como `FAILED`
- Garante exactly-once delivery mesmo com falhas

**Vantagens**:
- Desacopla transação de negócio (reserva) do envio de email
- Permite retry automático
- Auditoria completa de tentativas de envio

### 4. Flexibilidade para Múltiplos Canais

**Design**:
- Interface `RegistrationNotifier` permite implementar novos canais (WhatsApp, SMS, Slack)
- `EmailNotifier` implementa envio por email
- Factory pattern no `NotificationOutboxProcessor` seleciona notificador por canal

**Exemplo para adicionar WhatsApp**:
```java
@Component
public class WhatsAppNotifier implements RegistrationNotifier {
    @Override
    public boolean supports(String channel) {
        return "WHATSAPP".equals(channel);
    }
    
    @Override
    public void send(Registration registration) {
        // Implementar envio via API WhatsApp
    }
}
```

### 5. Validação e Tratamento de Erros

- DTOs com validações `@NotNull`, `@Email`, `@NotBlank`
- `GlobalExceptionHandler` centraliza respostas de erro
- Status HTTP apropriados: `201 CREATED`, `400 BAD REQUEST`, `404 NOT FOUND`, `409 CONFLICT`

## Trade-offs

| Decisão | Benefício | Custo |
|---------|-----------|-------|
| H2 em Memória | Rápido para testes, sem setup | Não persiste entre restarts |
| Pessimistic Locking | Garante atomicidade | Pode impactar throughput em alta concorrência |
| Outbox Pattern | Confiável, auditável | Complexidade adicional, tabela extra |
| Scheduled Tasks | Simples de implementar | Não horizontalmente escalável (usar message broker em produção) |

## O que Faria Diferente com Mais Tempo

### 1. Message Broker (RabbitMQ / Kafka)
Substituir `@Scheduled` por um message broker para processamento de notificações ser verdadeiramente distribuído e escalável.

### 2. Cache Distribuído
Adicionar Redis para cache de eventos frequentemente acessados e session store para suportar múltiplas instâncias do backend.

### 3. Observabilidade
- Adicionar Micrometer / Prometheus para métricas
- Implementar distributed tracing com Jaeger
- Structured logging com JSON

### 4. API Gateway
Implementar rate limiting e autenticação OAuth 2.0 em um API Gateway (Spring Cloud Gateway).

### 5. Testes de Carga
Usar JMeter ou Gatling para validar comportamento sob 10k+ requisições simultâneas.

### 6. Database Connection Pooling Tuning
Ajustar `HikariCP` conforme esperado de carga (maximumPoolSize, minimumIdle).

## Uso de IA no Projeto

### Como IA foi utilizada:

1. **Geração de código boilerplate** (DTOs, entities, repositories)
   - Aceitei: estrutura base estava correta
   - Ajustei: nomes e validações específicas do domínio

2. **Sugestão de patterns**
   - Pessimistic Locking: IA sugeriu, validei contra docs Spring Data JPA
   - Outbox Pattern: IA listou opções, escolhi esta baseado em análise de trade-offs

3. **Testes unitários**
   - IA gerou stubs, completei com mocks e assertions reais

4. **Configuração YAML**
   - IA gerou estrutura, ajustei properties específicas (CORS, mail)

### Decisões que descartei:

- ❌ Usar Optimistic Locking com `@Version`: menos garantista que Pessimistic
- ❌ REST API com GraphQL: over-engineering para escopo
- ❌ Saga Pattern distribuído: complexidade não justificada em first version

## Endpoints

### Listar Eventos
```
GET /api/events
```

### Obter Evento por ID
```
GET /api/events/{id}
```

### Registrar Participante
```
POST /api/registrations
Headers:
  Content-Type: application/json
  Idempotency-Key: <uuid>
  
Body:
{
  "eventId": 1,
  "participantName": "João Silva",
  "email": "joao@example.com"
}

Response: 201 CREATED
{
  "id": 10,
  "eventId": 1,
  "eventName": "Java Workshop",
  "participantName": "João Silva",
  "email": "joao@example.com",
  "status": "CONFIRMED",
  "createdAt": "2026-07-21T10:30:45Z"
}
```

## Estrutura do Banco de Dados

```sql
-- Eventos disponíveis
CREATE TABLE event (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  event_date TIMESTAMP,
  total_slots INT,
  available_slots INT,
  version BIGINT
);

-- Registros de participantes
CREATE TABLE registration (
  id BIGINT PRIMARY KEY,
  event_id BIGINT FOREIGN KEY,
  participant_name VARCHAR(255),
  email VARCHAR(150) UNIQUE (event_id, email),
  status VARCHAR(50),
  created_at TIMESTAMP
);

-- Fila de notificações
CREATE TABLE notification_outbox (
  id BIGINT PRIMARY KEY,
  registration_id BIGINT FOREIGN KEY,
  channel VARCHAR(50),
  status VARCHAR(50),
  attempts INT,
  sent_at TIMESTAMP,
  created_at TIMESTAMP
);
```

## Observações

- Aplicação já inclui dados iniciais via Flyway migrations (`V1__create_tables.sql`, `V2__initial_event_data.sql`)
- CORS configurado para aceitar `http://localhost:*` e `http://127.0.0.1:*` em desenvolvimento
- Email é enviado de forma assíncrona via scheduler (5s de intervalo)
