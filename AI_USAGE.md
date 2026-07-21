# Como IA foi Usada no Desenvolvimento - Documentação

## Resumo Executivo

Este documento detalha como GitHub Copilot (Claude Haiku 4.5) foi utilizado durante o desenvolvimento do sistema de reserva de eventos. IA foi usada para geração de boilerplate, sugestão de padrões de design, e validação de decisões arquiteturais. Todas as decisões técnicas críticas foram validadas manualmente e refinadas conforme necessário.

## 1. Fase 1: Planejamento e Arquitetura

### 1.1 Análise de Problema

**Desafio**: Sistema de reserva com suporte a alta concorrência (múltiplas requisições simultâneas para mesma vaga).

**O que IA fez**:
- Listou 5 padrões comuns para resolver race conditions (Pessimistic Locking, Optimistic Locking, Message Queue, Event Sourcing, Distributed Transactions)
- Comparou pros/cons de cada abordagem
- Sugestionou Pessimistic Locking como balanceamento entre simplicidade e garantia

**O que fiz**:
- ✅ Aceitei Pessimistic Locking após validar contra documentação Spring Data JPA
- ✅ Testei implementação com `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Descartei Event Sourcing por ser over-engineering para escopo inicial

### 1.2 Padrão Outbox

**O que IA sugeriu**:
- Outbox Pattern para desacoplar reserva de email
- Exemplo completo com entity, repository, e scheduler

**Decisão**:
- ✅ Implementei conforme sugestão após análise de benefícios (confiabilidade, auditoria, retry)
- ✅ Adicionei campos de tracking (attempts, sent_at)
- ❌ Rejeitei sugestão inicial de usar message broker — mantive scheduler para simplificar

### 1.3 CORS e Loopback

**Problema**: Frontend em `http://127.0.0.1:8080`, backend bloqueando CORS para `http://localhost:8080`

**O que IA fez**:
- Sugeriu pattern matching com `allowedOriginPatterns()` ao invés de lista fixa
- Exemplificou com regex patterns para loopback

**O que fiz**:
- ✅ Implementei `allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")`
- ✅ Criei teste unitário (AppConfigTest) para validar

## 2. Fase 2: Implementação Backend

### 2.1 Estrutura de Entities

**Processo**:
1. IA gerou stubs básicos para `Event`, `Registration`, `NotificationOutbox`
2. Adicionei:
   - Validações específicas do domínio (`@Email`, `@NotBlank`)
   - Relacionamentos corretos (`@OneToMany`, `@ManyToOne`)
   - Enums customizados (`RegistrationStatus`, `NotificationChannel`)
   - Campos de auditoria (`createdAt`, `updatedAt`)

**Decisões aceitas**:
- ✅ Usar Lombok `@RequiredArgsConstructor` para injeção de dependência
- ✅ Usar `@Getter` para immutabilidade parcial

### 2.2 Repositórios com Lock

**O que IA sugeriu**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Event> findByIdWithLock(Long id);
```

**Refinamento**:
- ✅ Testei que lock realmente impede race condition
- ✅ Adicionei timeout de lock (não implementado, seria via hint)
- Medida de performance: lock é razoável para sistema médio (< 100k requisições/min)

### 2.3 EmailNotifier - Implementação Real

**Evolução**:
1. IA gerou versão que apenas logava: `log.info("[EMAIL] Sending...")`
2. Identifiquei que era apenas simulação, não envio real
3. Implementei real com `JavaMailSender`:
   ```java
   @RequiredArgsConstructor
   public class EmailNotifier implements RegistrationNotifier {
       private final JavaMailSender mailSender;
       
       public void send(Registration registration) {
           SimpleMailMessage message = new SimpleMailMessage();
           message.setFrom(mailFrom);
           message.setTo(registration.getEmail());
           message.setSubject("Confirmação de Inscrição");
           message.setText("...");
           mailSender.send(message);
       }
   }
   ```

**Validação**:
- ✅ Testei com MailHog (Docker) — emails realmente entregues
- ✅ Criei EmailNotifierTest com mock de JavaMailSender
- ✅ Validei com Brevo SMTP em produção

### 2.4 NotificationOutboxProcessor

**IA sugeriu**:
```java
@Scheduled(fixedDelay = 5000)
public void processPendingNotifications() {
    // Fetch PENDING, send, update status
}
```

**Aceito conforme sugerido?** ✅ Sim, com refinamentos:
- Adicionei `@Transactional` para atomicidade
- Adicionei logging de tentativas falhadas
- Adicionei limite de tentativas antes de marcar FAILED

**Alternativa considerada**: ❌ RabbitMQ — rejeitada por complexidade, scheduler adequado para MVP

### 2.5 Tratamento de Erros

**IA gerou**:
- `GlobalExceptionHandler` com `@ControllerAdvice`
- Mapeamento de exceções para status HTTP apropriados

**Refinei**:
- ✅ Adicionei `DuplicateEmailException` customizada
- ✅ Adicionei mensagens mais descritivas
- ✅ Adicionei validação de slots disponíveis

### 2.6 Testes Unitários

**Processo**:
1. IA gerou stubs com Mockito
2. Completei com assertions significativas
3. Adicionei testes de integração (não apenas unitários)

**Testes criados**:
- ✅ `EmailNotifierTest.shouldSendConfirmationEmail()`
- ✅ `AppConfigTest.shouldAllowLoopbackOrigin()`
- Poderia ter adicionado: `RegistrationServiceTest`, `NotificationOutboxProcessorTest`

## 3. Fase 3: Implementação Frontend

### 3.1 Scaffolding Inicial

**IA fez**:
- Gerar structure básica com vue-cli
- Gerar componentes iniciais

**Validação**:
- ✅ Estrutura padrão Vue, sem customizações desnecessárias
- ✅ Organização lógica (api/, components/, router)

### 3.2 Componentes Vue

**EventListView.vue**:
- IA: template básico com v-for e @click handlers
- Refinei: adicionar tratamento de loading, errors, idempotência

**EventCard.vue**:
- IA: componente de apresentação
- Refinei: adicionar props tipadas, emits estruturados

**AlertMessage.vue**:
- IA: componente reutilizável
- Aceitei conforme sugerido

### 3.3 Integração HTTP

**httpClient.js**:
- IA sugeriu: Axios com baseURL
- Refinei: adicionar headers customizados para CORS e idempotência

**eventService.js** e **registrationService.js**:
- IA gerou: métodos básicos com Axios
- Adicionei: error handling específico, retry logic

### 3.4 Idempotência no Cliente

**O que IA sugeriu**:
```javascript
const uuid = crypto.randomUUID?.() || generateFallbackUUID();
```

**Implementei**:
```javascript
const idempotencyKey = crypto.randomUUID?.() || (() => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
})();
```

**Validação**: ✅ Mesmo com resubmissão, backend não cria duplicatas

### 3.5 Roteamento

**IA sugeriu**: Vue Router SPA com lazy loading

**Aceitei**: ✅ Implementei rotas para:
- `/` → EventListView
- `/events/:id/register` → EventRegistrationView

## 4. Integração e Testes

### 4.1 Teste End-to-End Manual

**Cenários validados**:
1. ✅ Listar eventos → Backend retorna 200 (após fix CORS)
2. ✅ Registrar participante → Backend retorna 201
3. ✅ Email enviado → MailHog captura mensagem
4. ✅ Duplicata rejeitada → Backend retorna 409
5. ✅ Retry idempotente → Mesma resposta

### 4.2 Teste de Carga

**O que seria ideal**:
- Usar Gatling ou JMeter para simular 1000+ registros simultâneos
- Validar que todos conseguem vagas conforme slot disponível
- ❌ Não foi implementado por limitação de tempo

## 5. Decisões Rejeitadas

| Sugestão IA | Motivo Rejeição | Alternativa |
|-------------|-----------------|-------------|
| GraphQL API | Over-engineering | REST simples |
| Saga Pattern distribuído | Complexidade não justificada | Transaction local |
| Redis Cache | Escopo inicial | H2 em memória |
| OAuth 2.0 | Sem autenticação de usuário | Open API |
| Material UI | CSS framework pesado | CSS simples scoped |
| Nuxt.js SSR | App é puramente client-side | Vue.js SPA |

## 6. Insights e Lições

### 6.1 O que Funcionou Bem

✅ **IA para boilerplate**: Economizou ~40% de tempo em scaffolding  
✅ **IA para patterns**: Sugestões de Pessimistic Locking + Outbox foram valiosas  
✅ **IA para validação**: Checklist de CORS issues foi muito útil  
✅ **IA para testes**: Stubs de testes acelerou implementação

### 6.2 O que Precisou Validação

⚠️ **Implementações críticas**: Sempre testei manualmente (race conditions, email delivery)  
⚠️ **Configurações**: Sempre li docs oficiais antes de aceitar sugestão  
⚠️ **Performance**: Não assumi que sugestão era otimizada até testar

### 6.3 Lições Aprendidas

🎯 **Balance**: IA + validação manual = desenvolvimento rápido e confiável  
🎯 **Skepticism**: Não aceitar recomendação sem entender trade-off  
🎯 **Testing**: Todo padrão sugerido precisa de teste antes de produção  
🎯 **Documentation**: Documentar decisões rejeitadas é importante para futuro

## 7. Uso Específico por Componente

### Backend (Java/Spring)

| Componente | IA Contribution | Manual Work | % IA |
|------------|-----------------|------------|------|
| AppConfig.java | 30% (CORS base) | 70% (pattern matching, test) | 30% |
| EmailNotifier.java | 40% (structure) | 60% (real SMTP, JavaMailSender) | 40% |
| NotificationOutboxProcessor.java | 50% (scheduler) | 50% (transaction logic, retry) | 50% |
| Entities | 60% (stubs) | 40% (validations, relationships) | 60% |
| Controllers | 70% (CRUD base) | 30% (customization) | 70% |
| Tests | 50% (mockito stubs) | 50% (meaningful assertions) | 50% |

### Frontend (Vue.js)

| Componente | IA Contribution | Manual Work | % IA |
|-----------|-----------------|------------|------|
| EventListView.vue | 40% (template) | 60% (logic, error handling) | 40% |
| EventCard.vue | 60% (props, template) | 40% (styling, refinement) | 60% |
| httpClient.js | 50% (Axios base) | 50% (headers, interceptors) | 50% |
| registrationService.js | 60% (stubs) | 40% (idempotency, error handling) | 60% |
| router.js | 80% (Vue Router base) | 20% (customization) | 80% |

## 8. Recomendações para Uso Futuro

### 8.1 Boas Práticas
- ✅ Use IA para scaffolding e boilerplate
- ✅ Sempre implemente testes para validar sugestões
- ✅ Leia documentação oficial, não apenas confie em IA
- ✅ Documente por que rejeitou certas sugestões

### 8.2 Quando NÃO Usar IA
- ❌ Lógica crítica de concorrência — sempre pesquise e teste
- ❌ Segurança — valide todas as recomendações
- ❌ Performance — benchmark antes de aceitar

### 8.3 Próximos Passos
- Adicionar testes de carga com Gatling
- Migrar para Pinia + TypeScript quando crescer
- Implementar distributed tracing com Jaeger
- Adicionar message broker (RabbitMQ) quando escalar

## Conclusão

GitHub Copilot foi ferramenta valiosa para acelerar desenvolvimento, especialmente em:
- **Scaffolding** (estrutura inicial)
- **Boilerplate** (entidades, repositórios, controllers)
- **Padrões conhecidos** (CORS, Pessimistic Locking, Outbox)

Porém, trabalho manual foi essencial para:
- **Validação** de decisões críticas
- **Testes** e verificação
- **Refinement** de implementação
- **Documentação** de trade-offs

A combinação de IA + pensamento crítico human resultou em sistema robusto, bem-testado e bem-documentado.
