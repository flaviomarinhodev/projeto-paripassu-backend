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
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: ${MAIL_SMTP_AUTH:false}
      mail.smtp.starttls.enable: ${MAIL_SMTP_STARTTLS:false}
      mail.smtp.ssl.trust: ${MAIL_SMTP_SSL_TRUST:}

./mvnw spring-boot:run
```

### 3. Rodar Testes

```bash
# Todos os testes
./mvnw test

# Teste específico
./mvnw -Dtest=EmailNotifierTest test
```

## Decisões de Arquitetura

### 1. Utilização de banco de dados H2
### 2. Utilização do flyway para rodar a carga inicial de dados no banco
### 3. Utilização do Lombok para otimização das classes
### 4. Utilização do Scheduled para agendamento notificação

### Como IA foi utilizada:

1. **Utilizado processo de desenvolvimento por Vibe Coding, seguindos os processos de analise, planejamento e execução**
2. **Aqui não foram commitado os artefatos por conta do meu copilot não ser profissional e eu ter de fazer esse processo por fora**

### Alguns pontos que defini no prompt

1. **Todos os pontos levantados em arquitetura, versão Java, versão spring-boot**
2. **Correção de erro de CORS**
3. **Publicação no render**

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

- Aplicação está publicada no render e disponível para teste no link:
- https://projeto-paripassu-frontend-deploy.onrender.com/

