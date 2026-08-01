# Spring Boot Microservice Base Project

Template Spring Boot per sperimentare e riusare pratiche comuni nei microservizi: API REST, validazione, sicurezza, persistence con JPA/Flyway, cache, chiamate HTTP, resilienza, osservabilita, idempotenza dei pagamenti, transactional outbox, virtual thread, graceful shutdown, method security, hardening degli header di sicurezza, auditing JPA e test di architettura.

## Prerequisiti

- JDK 25, coerente con la proprieta `java.version` del `pom.xml`.
- Git e un terminale PowerShell, Bash o compatibile.
- Non serve installare Maven: il progetto usa Maven Wrapper (`mvnw` / `mvnw.cmd`).
- Docker Desktop con Docker Compose, necessario per lo stack di osservabilita e per eseguire localmente i test Testcontainers PostgreSQL.

Verificare che il terminale usi il JDK corretto:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd --version
```

Su Linux/macOS:

```bash
export JAVA_HOME=/percorso/al/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw --version
```

## Avvio locale

L'applicazione usa H2 in memoria per impostazione predefinita. Flyway applica automaticamente le migrazioni all'avvio e Hibernate esegue solo la validazione dello schema.

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

In alternativa, creare il jar ed eseguirlo:

```powershell
.\mvnw.cmd clean package
java -jar target\ms-base-prj-0.0.1-SNAPSHOT.jar
```

L'API applicativa e disponibile su `http://localhost:8080`; gli endpoint di management sono sulla porta `8081`.

## Profili

- `demo` (default): H2 in memoria, Basic Auth configurabile tramite `DEMO_USER` e `DEMO_PASSWORD`, dataset Flyway e receiver webhook interno per provare l'outbox.
- `production`: OAuth2 Resource Server con JWT, CORS esplicito e accesso Actuator protetto dallo scope `actuator.read`.
- `dev`: abilita DevTools, logging Hibernate dettagliato e sampling tracing al 100%.
- `rabbitmq`: instrada l'outbox verso RabbitMQ (producer + consumer end-to-end) invece che verso il webhook; combinabile con gli altri, es. `demo,rabbitmq`. Vedi "Outbox su broker RabbitMQ".

Attivare il profilo di sviluppo:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Il profilo `dev` non sostituisce la configurazione di produzione: per un ambiente reale impostare esplicitamente datasource, segreti, livelli di logging e indirizzo della porta management.

Per il profilo production sono obbligatorie `OAUTH2_ISSUER_URI`, `API_KEY` e `CORS_ALLOWED_ORIGINS` (lista separata da virgole).

## Configurazione

Le proprieta sono in `src/main/resources/application.yml` e possono essere sovrascritte tramite variabili d'ambiente. I valori di esempio non sono segreti validi per la produzione.

| Variabile | Uso | Note |
| --- | --- | --- |
| `SERVER_PORT` | Porta API | Default `8080`. |
| `MANAGEMENT_SERVER_PORT` | Porta Actuator | Default `8081`; limitarla a rete interna. |
| `SPRING_PROFILES_ACTIVE` | Profilo Spring | Usare `dev` solo in locale. |
| `THREADS_VIRTUAL_ENABLED` | Abilita i virtual thread (Java 21+/25) | Default `true`; una richiesta per virtual thread invece del pool di platform thread. |
| `SHUTDOWN_TIMEOUT_PER_PHASE` | Timeout massimo di drain allo shutdown graceful | Default `20s`. |
| `RATE_LIMIT_ENABLED` | Abilita il rate limiter delle richieste in ingresso su `/api/**` | Default `true`. |
| `RATE_LIMIT_CAPACITY` | Numero massimo di richieste (burst) per client prima del throttling | Default `20`. |
| `RATE_LIMIT_REFILL_PERIOD` | Finestra ISO-8601 in cui la capacita viene ricaricata | Default `PT1M` (un minuto). |
| `EXTERNAL_API_BASE_URL` | Base URL del gateway esterno | Da configurare per integrazioni reali. |
| `EXTERNAL_API_SUBSCRIPTION_KEY` | Credenziale del gateway | Fornirla con secret store o variabile protetta. |
| `EXTERNAL_API_CONNECTION_TIMEOUT_MILLIS` | Timeout connessione HTTP | Default `500`. |
| `EXTERNAL_API_RESPONSE_TIMEOUT_MILLIS` | Timeout risposta HTTP | Default `3000`. |
| `API_KEY` | Chiave applicativa prevista dalla configurazione | Non lasciare il valore di esempio in produzione. |
| `SENTRY_DSN` | DSN Sentry | Richiesta solo se `SENTRY_ENABLED=true`. |
| `SENTRY_ENABLED` | Abilita Sentry | Default `false`. |
| `MANAGEMENT_TRACING_ENABLED` | Abilita tracing Micrometer | Default `true`. |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Sampling tracing | Default `0.2`. |
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | Endpoint OTLP per le trace | Per il Collector locale usare `http://localhost:4318/v1/traces`. |
| `OUTBOX_POLL_INTERVAL_MS` | Frequenza polling outbox | Default `1000`. |
| `OUTBOX_ASYNC_DEMO_ENABLED` | Abilita demo `CompletableFuture` | Default `false`; non influenza la consegna outbox. |
| `OUTBOX_WEBHOOK_ENABLED` | Abilita il publisher webhook dell'outbox | Default base `false`; nel profilo `demo` e `true`. |
| `OUTBOX_WEBHOOK_BASE_URL` | Base URL del consumer webhook | Nel profilo `demo` usa il receiver interno; negli altri profili e richiesta quando il webhook e abilitato. |
| `OUTBOX_RABBITMQ_EXCHANGE` | Topic exchange dell'outbox RabbitMQ | Default `outbox.events`; usato solo col profilo `rabbitmq`. |
| `OUTBOX_RABBITMQ_QUEUE` | Coda demo dell'outbox RabbitMQ | Default `outbox.events.demo`; usato solo col profilo `rabbitmq`. |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | Connessione al broker RabbitMQ | Default `localhost` / `5672`. |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Credenziali RabbitMQ | Default `guest` / `guest`. |
| `MANAGEMENT_HEALTH_RABBIT_ENABLED` | Abilita l'health indicator RabbitMQ | Default `false`; nel profilo `rabbitmq` e `true`. |

Le opzioni Hikari, Hibernate e del client HTTP sono configurabili con le rispettive variabili esposte nel file YAML.

## Runtime: virtual thread e graceful shutdown

Su Java 25 il progetto abilita i **virtual thread** con `spring.threads.virtual.enabled=true` (variabile `THREADS_VIRTUAL_ENABLED`): Tomcat serve ogni richiesta su un virtual thread, eliminando il limite del pool di platform thread per carichi I/O-bound. Su JDK 24+ i blocchi `synchronized` non causano piu pinning, quindi l'abilitazione e sicura per le librerie usate. L'`appTaskExecutor` di `@Async` resta un pool bounded per applicare backpressure dove serve.

Lo **shutdown graceful** e attivo con `server.shutdown=graceful`: alla ricezione del segnale di stop l'applicazione smette di accettare nuove richieste e lascia terminare quelle in corso entro `spring.lifecycle.timeout-per-shutdown-phase` (variabile `SHUTDOWN_TIMEOUT_PER_PHASE`, default `20s`). In Kubernetes va coordinato con `readinessProbe` e `preStop` per drenare il traffico prima della terminazione.

## Rate limiting delle richieste in ingresso

Le richieste su `/api/**` passano da un `RateLimitInterceptor` che applica un algoritmo **token bucket** (libreria Bucket4j) per proteggere il servizio da burst e abusi. Ogni client (identificato dal primo hop di `X-Forwarded-For`, altrimenti dall'indirizzo remoto) ha un bucket con capacita `RATE_LIMIT_CAPACITY` (default `20`), ricaricato in modo `greedy` sull'intera finestra `RATE_LIMIT_REFILL_PERIOD` (default `PT1M`). I bucket sono mantenuti in memoria: per un deployment multi-istanza va sostituito con uno store distribuito (ad esempio Bucket4j su Redis/Hazelcast).

Quando un client esaurisce i token la richiesta viene respinta con **`429 Too Many Requests`**, corpo RFC 7807 `application/problem+json` e header `Retry-After` con i secondi di attesa; ogni risposta consentita espone `X-RateLimit-Remaining`. Il rigetto e scritto direttamente dall'interceptor (non tramite eccezione) perche le eccezioni sollevate da un interceptor non attraversano in modo affidabile `@RestControllerAdvice`. I rigetti sono contati nella metrica `http.rate_limit{outcome="rejected"}`. Il limiter puo essere disattivato con `RATE_LIMIT_ENABLED=false`.

## Sicurezza

Gli endpoint `/payments/**` e `/api/secured/**` richiedono HTTP Basic. Il template include un utente dimostrativo `user` / `password` esclusivamente per sviluppo e test; non usare queste credenziali in alcun ambiente condiviso.

Prima della produzione:

1. sostituire l'utente in memoria con un identity provider, ad esempio OAuth2 Resource Server e JWT;
2. usare HTTPS terminato in modo affidabile;
3. caricare tutte le credenziali da un secret store;
4. restringere l'accesso alla porta management con rete/firewall;
5. mantenere disabilitati logging HTTP verboso e console H2.

### Autorizzazione a livello di metodo e header di sicurezza

Oltre alle regole URL-based dei filter chain, `@EnableMethodSecurity` abilita l'autorizzazione a livello di metodo: ad esempio `BalanceService.getBalance()` e annotato con `@PreAuthorize("isAuthenticated()")`, cosi la regola viene applicata vicino alla logica di business e indipendentemente dal punto di ingresso.

Entrambi i profili impostano header di sicurezza sulle risposte. Il profilo `demo` usa una Content-Security-Policy permissiva quanto basta per la Swagger UI, piu `Referrer-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options` e HSTS. Il profilo `production` applica una CSP piu restrittiva (`default-src 'none'`), `frame-ancestors 'none'`, `Referrer-Policy: no-referrer` e HSTS con `preload`.

### Pagamenti

`POST /payments` richiede autenticazione Basic, header `Idempotency-Key` e un body validato. L'importo usa `BigDecimal` e la valuta deve essere un codice ISO 4217.

```http
POST /payments
Authorization: Basic <credenziali>
Idempotency-Key: payment-0001
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "cardHolder": "Mario Rossi",
  "expirationDate": "12/30",
  "cvv": "123",
  "amount": 19.99,
  "currency": "EUR"
}
```

La stessa chiave e la stessa richiesta restituiscono la risposta precedentemente completata senza una seconda chiamata al gateway. La riutilizzazione della chiave con dati differenti restituisce `409 Conflict`. PAN e CVV sono mascherati nei log HTTP; in un sistema conforme PCI la tokenizzazione deve avvenire prima che questi dati raggiungano il servizio.

Le chiamate al gateway sono protette da Retry, Circuit Breaker e Bulkhead Resilience4j. Il retry riguarda solo errori transitori del gateway o di rete e conserva sempre la stessa `Idempotency-Key`; errori di validazione e rate limit non vengono ritentati. Le soglie possono essere adattate tramite le variabili `RESILIENCE4J_PAYMENT_GATEWAY_*` definite in `application.yml`.

### Outbox webhook

L'outbox persiste l'evento nella stessa transazione dell'ordine, lo acquisisce con lease e lo pubblica con retry esponenziale. Se si abilita il publisher webhook, ogni evento viene inviato come `POST {OUTBOX_WEBHOOK_BASE_URL}/events` con:

- `Idempotency-Key`: UUID dell'evento outbox;
- `X-Event-Type`: tipo dell'evento;
- body: payload JSON persistito nell'outbox.

Una risposta HTTP non `2xx` fa fallire la consegna e attiva il retry; dopo cinque tentativi l'evento viene spostato nella dead-letter persistente. Il consumer webhook deve trattare `Idempotency-Key` come chiave di deduplicazione.

Il profilo `demo` carica anche un utente Flyway per provare l'outbox: `demo-outbox@example.test`. Dopo l'avvio, recuperarne l'ID con `GET /api/user?email=demo-outbox@example.test` e usarlo in `POST /api/orders`.

Nel profilo `demo` il publisher webhook e attivo e invia al receiver interno `POST /api/demo/webhook/events`, che risponde `204 No Content`. Per usare un consumer esterno, impostare `OUTBOX_WEBHOOK_BASE_URL`; per disabilitare il publisher, impostare `OUTBOX_WEBHOOK_ENABLED=false`.

### Outbox su broker RabbitMQ

Oltre all'adapter webhook, l'outbox puo consegnare gli eventi a un **broker RabbitMQ**, attivabile a richiesta con il profilo Spring dedicato **`rabbitmq`**. La meccanica di base resta la stessa (persistenza transazionale, lease, retry esponenziale, dead-letter, metriche): cambia solo l'adapter di consegna. Col profilo attivo il publisher RabbitMQ e l'**unico** attivo, mentre quello webhook e disattivato (mutua esclusione), cosi un solo scheduler acquisisce gli eventi.

Il flusso e end-to-end all'interno della stessa applicazione demo:

1. **Producer** (`RabbitMqOutboxPublisher`): pubblica il payload dell'evento su un topic exchange (`outbox.events`) con routing key uguale al tipo evento (es. `order.created`), impostando `messageId` = UUID dell'evento (chiave di idempotenza downstream), header `X-Event-Type` e `contentType application/json`.
2. **Topologia** (`RabbitMqOutboxConfig`): dichiara exchange, coda demo (`outbox.events.demo`) e binding `#`; sono auto-dichiarati sul broker all'avvio.
3. **Consumer** (`OutboxEventRabbitListener`): un `@RabbitListener` riceve i messaggi dalla coda, li logga e incrementa la metrica `outbox.events.consumed`, dimostrando il consumo.

Nomi di exchange e coda sono configurabili con `OUTBOX_RABBITMQ_EXCHANGE` e `OUTBOX_RABBITMQ_QUEUE`; la connessione con `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`. Poiche lo starter AMQP e sempre sul classpath, il relativo health indicator e disabilitato per default (`MANAGEMENT_HEALTH_RABBIT_ENABLED=false`) e riabilitato solo nel profilo `rabbitmq`, per non far tentare la connessione al broker quando non serve.

Per provarlo in locale, avviare RabbitMQ con il compose dedicato e poi l'app col profilo attivo:

```powershell
docker compose -f compose-rabbitmq.yaml up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo,rabbitmq"
```

```bash
docker compose -f compose-rabbitmq.yaml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo,rabbitmq
```

La management UI e su `http://localhost:15672` (`guest`/`guest`). Generando un ordine (vedi "Outbox Demo") l'evento viene pubblicato su RabbitMQ e consumato dal listener interno. Il test di integrazione `RabbitMqOutboxEndToEndIntegrationTest` (Testcontainers, taggato `testcontainers`) verifica automaticamente l'intero flusso producer-broker-consumer.

## Mapping DTO con MapStruct

Il progetto usa **MapStruct** per generare a compile time il mapping tra entita JPA e modelli API, evitando conversioni manuali verbose e ripetitive. `UserMapper` (`@Mapper(componentModel = "spring")`) mappa l'entita `User` sul record `UserDto`; l'implementazione (`UserMapperImpl`) e generata dall'annotation processor `mapstruct-processor` (configurato in `pom.xml` tramite `annotationProcessorPaths`) e registrata come bean Spring, quindi iniettabile come qualsiasi componente. `UserController.getUser(id)` recupera l'utente dal repository e lo converte con il mapper, restituendo `404` se assente.

Il mapper imposta `unmappedTargetPolicy = ERROR`: se una proprieta del target non viene mappata, la compilazione fallisce. Questo rende esplicito e verificato ogni campo esposto dall'API, spostando a build time errori che altrimenti emergerebbero a runtime.

## Versioning delle API

Il progetto adotta il **versioning per URI**: ogni versione major e esposta con un prefisso di percorso dedicato (`/api/v1/...`, `/api/v2/...`). La risorsa dimostrativa `greetings` mostra un'evoluzione additiva: `GET /api/v1/greetings` restituisce solo `message`, mentre `GET /api/v2/greetings` restituisce una rappresentazione arricchita (`message`, `language`, `apiVersion`). Le due versioni sono servite da controller distinti, cosi i client migrano al proprio ritmo e la v1 resta stabile.

Il versioning per URI e esplicito, cacheable e semplice da instradare a livello di gateway. In alternativa si possono usare header custom o content negotiation (`Accept: application/vnd.example.v2+json`): sono piu puliti per l'URL ma meno visibili e piu difficili da testare a mano. La regola pratica: cambi additivi restano nella stessa versione, cambi breaking introducono una nuova versione major.

## Osservabilita

Actuator e esposto sulla porta management:

| Endpoint | Scopo |
| --- | --- |
| `/actuator/health` | Stato complessivo dell'applicazione. |
| `/actuator/health/liveness` | Verifica liveness JVM. |
| `/actuator/health/readiness` | Verifica readiness delle dipendenze. |
| `/actuator/prometheus` | Metriche Prometheus. |
| `/actuator/metrics` | Elenco e dettaglio metriche. |
| `/actuator/circuitbreakers` | Stato circuit breaker Resilience4j. |
| `/actuator/httpexchanges` | Ultimi scambi HTTP in memoria. |
| `/actuator/threaddump` | Thread dump diagnostico. |

Gli endpoint esposti includono informazioni operative sensibili: non pubblicarli su Internet e applicare un'autorizzazione dedicata. I log strutturati contengono trace/span ID quando il tracing e disponibile.

Metriche applicative rilevanti:

| Metrica | Tag | Significato |
| --- | --- | --- |
| `payments.idempotency` | `outcome=replay` | Risposta riprodotta senza richiamare il gateway. |
| `payments.idempotency` | `outcome=conflict` | Chiave riusata con una richiesta differente. |
| `payments.idempotency` | `outcome=reclaimed` | Lease di pagamento scaduto recuperato. |
| `outbox.events` | `outcome=claimed` | Eventi acquisiti dal publisher. |
| `outbox.events` | `outcome=published` | Eventi consegnati e marcati processati. |
| `outbox.events` | `outcome=retried` | Consegne pianificate per un nuovo tentativo. |
| `outbox.events.dead-lettered` | - | Eventi spostati in dead-letter. |
| `outbox.events.consumed` | - | Eventi ricevuti dal consumer RabbitMQ (profilo `rabbitmq`). |

### Stack locale di osservabilita

`compose.yaml` avvia Prometheus, Grafana, Tempo e OpenTelemetry Collector. Avviare lo stack e l'applicazione con l'endpoint OTLP configurato:

```powershell
docker compose up -d
$env:MANAGEMENT_OTLP_TRACING_ENDPOINT = 'http://localhost:4318/v1/traces'
.\mvnw.cmd spring-boot:run
```

```bash
docker compose up -d
MANAGEMENT_OTLP_TRACING_ENDPOINT=http://localhost:4318/v1/traces ./mvnw spring-boot:run
```

Servizi locali:

| Servizio | URL | Note |
| --- | --- | --- |
| Prometheus | `http://localhost:9090` | Scrape di `host.docker.internal:8081/actuator/prometheus`. |
| Grafana | `http://localhost:3000` | Credenziali iniziali `admin` / `admin`, da cambiare. |
| Tempo | `http://localhost:3200` | Backend trace, provisionato come datasource Grafana. |
| OTel Collector | `localhost:4317` / `localhost:4318` | Riceve trace OTLP gRPC/HTTP e le inoltra a Tempo. |

Su Docker Engine Linux, `compose.yaml` configura `host.docker.internal:host-gateway` per permettere a Prometheus di raggiungere Actuator sull'host. Prometheus non richiede Basic Auth sulla porta `9090`; Actuator richiede invece le credenziali del profilo attivo.

Arrestare e rimuovere i container con `docker compose down`.

## Contratto API

Il contratto OpenAPI JSON e generato automaticamente da Springdoc durante l'avvio:

- JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`, disponibile nel profilo `demo`.

Nel profilo `production` la Swagger UI e disabilitata; il contratto JSON resta soggetto alle regole di sicurezza del profilo attivo.

## Contract testing (Spring Cloud Contract)

Il **contract testing consumer-driven** verifica che il contratto tra chi espone un'API (producer) e chi la consuma (consumer) resti stabile nel tempo, senza dover avviare entrambi i servizi insieme. Il progetto dimostra il lato **producer** con **Spring Cloud Contract** (SCC) sull'endpoint webhook `POST /api/demo/webhook/events`.

Il flusso e il seguente:

1. Il contratto e descritto in un DSL Groovy in `src/test/resources/contracts/webhook/shouldAcceptOutboxEvent.groovy`: definisce la richiesta attesa (metodo, path, header `Idempotency-Key` e `X-Event-Type`, body JSON) e la risposta attesa (`204 No Content`). E la fonte di verita condivisa tra producer e consumer.
2. In fase di build il plugin `spring-cloud-contract-maven-plugin` genera automaticamente un test JUnit 5 (`WebhookTest`) a partire dal contratto. Il test estende la classe base `WebhookContractBase`, che configura `RestAssuredMockMvc` in modalita standalone sul controller webhook: nessun server, nessun broker e nessun contesto Spring completo sono necessari.
3. Lo stesso contratto puo produrre uno **stub** (`generateStubs`) riutilizzabile dai consumer per testare il proprio codice contro una simulazione dell'API, senza dipendere dal producer reale.

Se l'implementazione del producer smette di rispettare il contratto (per esempio cambia lo status code o un header richiesto), il test generato fallisce durante `mvn test`, intercettando la regressione a build time. Il test e integrato nella suite standard e non richiede infrastruttura esterna, coerentemente con il perimetro single-service della demo. Il plugin e allineato al release train Spring Cloud `2025.0.0` (SCC `5.0.3`), compatibile con Spring Boot 3.5 e Java 25.

## Qualita e container

Il build standard applica Maven Enforcer e richiede Java 25. I quality gate aggiuntivi sono attivabili in CI o localmente:

```powershell
.\mvnw.cmd verify -Pquality
```

```bash
./mvnw verify -Pquality
```

Il profilo esegue Spotless, SpotBugs e OWASP Dependency-Check. SpotBugs usa un filtro mirato per le associazioni gestite da JPA e per oggetti Spring iniettati; le collezioni esposte dai DTO pubblici usano invece copie difensive. Costruire ed eseguire l'immagine multi-stage:

```powershell
docker build -t ms-base-prj .
docker run --rm -p 8080:8080 -p 8081:8081 ms-base-prj
```

## Database e migrazioni

Flyway usa gli script versionati in `src/main/resources/db/migration`.

- Aggiungere una nuova migrazione con nome `V<numero>__<descrizione>.sql`.
- Non modificare o cancellare una migrazione gia applicata in ambienti condivisi.
- Lasciare `spring.jpa.hibernate.ddl-auto=validate`: lo schema e di proprieta delle migrazioni, non di Hibernate.
- Eseguire test di integrazione anche sul database di produzione previsto; H2 in modalita PostgreSQL e utile localmente ma non sostituisce PostgreSQL reale.

Il progetto usa l'**auditing di Spring Data JPA** (`@EnableJpaAuditing`): l'entita `Order` e annotata con `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` e `@LastModifiedBy`, popolate automaticamente. L'autore ("chi") e risolto dal contesto di sicurezza tramite `AuditorAware`, con fallback a `system` per i flussi non autenticati. La migrazione `V9` aggiunge le colonne corrispondenti alla tabella `orders`.

L'outbox usa lease, retry esponenziale e dead-letter persistente. Un adapter concreto deve usare l'ID dell'evento come chiave di idempotenza per il broker o il consumer downstream.

## Test locali

Eseguire la suite completa:

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

I test pagamento usano un gateway HTTP locale e coprono validazione, autorizzazione, rate limit, timeout, 5xx, idempotenza e mascheramento dei dati sensibili.

### Piramide dei test

La suite segue la testing pyramid, scegliendo per ogni caso lo slice piu piccolo che copra il comportamento:

- **Unit test** puri, senza contesto Spring, per la logica isolata (ad esempio `HttpLoggingInterceptorTest`);
- **Slice test**, che caricano solo il layer necessario ed sono quindi veloci e mirati:
  - `@WebMvcTest` per il layer web (`HelloControllerTest`, `GreetingVersioningTest`): solo controller, serializzazione e filtri di sicurezza;
  - `@DataJpaTest` per la persistenza (`ProductRepositoryIntegrationTest`, `UserRepositorySliceTest`): entita, repository, datasource in memoria e transazione con rollback per test;
- **Integration test** con `@SpringBootTest` (piu `@AutoConfigureMockMvc` o `RANDOM_PORT`) quando serve il contesto completo end-to-end (ordini, pagamenti, outbox, rate limiter, virtual thread).

La regola pratica: salire di livello nella piramide solo quando lo slice inferiore non basta a verificare il comportamento; questo mantiene la suite rapida e i test focalizzati.

La suite include anche `ArchitectureTest`, basato su **ArchUnit**, che protegge le convenzioni di layering e di codice (i controller sono `@RestController`, i repository risiedono nel package `repository`, i service non dipendono dai controller, nessuna field injection, niente `System.out`/`java.util.logging`). Questi test girano nel normale `mvnw test` e fanno fallire il build in caso di violazione.

La suite include anche test espliciti per apertura del circuit breaker, saturazione del bulkhead e pubblicazione webhook end-to-end. Include inoltre `WebhookTest`, il **contract test** generato da Spring Cloud Contract sull'endpoint webhook (vedi la sezione "Contract testing"), che gira nel normale `mvnw test` senza infrastruttura esterna. I test PostgreSQL Testcontainers e il test end-to-end RabbitMQ (`RabbitMqOutboxEndToEndIntegrationTest`) sono taggati `testcontainers` ed esclusi dalla suite locale standard, per non richiedere Docker in ogni esecuzione. Per eseguirli dove Docker e disponibile:

```powershell
.\mvnw.cmd -Dtest.excludedGroups= -Dgroups=testcontainers test
```

```bash
./mvnw -Dtest.excludedGroups= -Dgroups=testcontainers test
```

GitHub Actions esegue sia la suite e i quality gate standard sia il job Testcontainers separato.

Per eseguire solo la suite pagamenti:

```powershell
.\mvnw.cmd test '-Dtest=PaymentFlowIntegrationTest,HttpLoggingInterceptorTest'
```

```bash
./mvnw test -Dtest=PaymentFlowIntegrationTest,HttpLoggingInterceptorTest
```

## Script di supporto

Sono disponibili script in `scripts/bat` e `scripts/sh` per build, run, live reload, smoke test e test. Gli script richiedono una variabile `JDK_PATH` nel loro file `.env`; impostare un percorso al JDK 25. Per flussi CI o per evitare ambiguita sul percorso di esecuzione, preferire i comandi Maven Wrapper riportati sopra dalla root del repository.

## Hook Git

Il repository include un hook `pre-commit` che esegue Spotless. Se applica modifiche, il commit viene bloccato affinche le modifiche siano revisionate e aggiunte esplicitamente allo stage. Git non condivide automaticamente la configurazione degli hook tra cloni: ogni sviluppatore deve abilitarla una volta dalla root del repository.

```bash
git config core.hooksPath .githooks
```

Il comando funziona sia in Git Bash su Windows sia su Linux/macOS. L'hook usa `./mvnw`, quindi richiede un JDK 25 configurato in `JAVA_HOME` o disponibile nel `PATH`.

Per applicare automaticamente la formattazione prima del commit:

```bash
./mvnw -Pquality spotless:apply
```

## Test Manuali

I comandi seguenti usano Bash, `curl` e `jq`. Su PowerShell impostare le stesse variabili con `$env:NOME_VARIABILE = 'valore'` e usare `curl.exe` se l'alias `curl` e associato a un altro comando.

### Verifica Osservabilita

Avviare lo stack dalla root del repository:

```bash
docker compose up -d
```

In un altro terminale avviare l'applicazione con trace campionate al 100%:

```bash
LOG_LEVEL_BASE=DEBUG \
SPRING_PROFILES_ACTIVE=demo \
MANAGEMENT_OTLP_TRACING_ENDPOINT=http://localhost:4318/v1/traces \
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0 \
./mvnw spring-boot:run
```

Generare traffico applicativo:

```bash
curl -sS "http://localhost:8080/api/hello?name=Linux"
```

`/api/hello` contiene una failure casuale per scopi dimostrativi e puo restituire `500` circa nel 30% delle chiamate.

Verificare Actuator con Basic Auth del profilo demo:

```bash
curl -sS -u user:password http://localhost:8081/actuator/prometheus
curl -sS -u user:password http://localhost:8081/actuator/health | jq -r '.status'
curl -sS -u user:password http://localhost:8081/actuator/health/liveness | jq -r '.status'
curl -sS -u user:password http://localhost:8081/actuator/health/readiness | jq -r '.status'
```

L'health aggregato puo risultare `DOWN` per l'indicatore demo della dipendenza esterna; liveness e readiness devono restare `UP`.

Verificare che Prometheus riesca a fare scrape dell'endpoint metriche:

```bash
curl -sG http://localhost:9090/api/v1/query \
  --data-urlencode 'query=up{job="ms-base-prj"}' |
jq -r '.data.result[0].value[1]'

curl -s http://localhost:9090/api/v1/targets |
jq '.data.activeTargets[] | select(.labels.job == "ms-base-prj") | {health, lastError}'
```

Il primo comando deve stampare `1`; questo indica la riuscita dello scrape, non lo stato dell'health aggregato. Aprire Prometheus in `http://localhost:9090/targets`, Grafana in `http://localhost:3000` con credenziali iniziali `admin` / `admin`, quindi cercare le trace in **Explore -> Tempo**. Cambiare la password Grafana al primo accesso.

### OpenAPI

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui/index.html
```

Il primo URL restituisce il documento OpenAPI JSON; il secondo apre Swagger UI nel profilo demo.

### Outbox Demo

Il profilo demo invia gli eventi al receiver webhook interno. Avviare l'applicazione:

```bash
SPRING_PROFILES_ACTIVE=demo OUTBOX_POLL_INTERVAL_MS=1000 ./mvnw spring-boot:run
```

Recuperare l'utente seed e creare un ordine:

```bash
USER_ID=$(curl -s 'http://localhost:8080/api/user?email=demo-outbox@example.test' | jq -r '.id')

curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":$USER_ID,\"amount\":19.99}"
```

Dopo circa un secondo verificare l'acquisizione e la consegna:

```bash
curl -s -u user:password \
  'http://localhost:8081/actuator/metrics/outbox.events?tag=outcome:claimed' |
jq '.measurements'

curl -s -u user:password \
  'http://localhost:8081/actuator/metrics/outbox.events?tag=outcome:published' |
jq '.measurements'
```

I contatori sono cumulativi per l'intero processo: confrontare il loro valore prima e dopo il test. Nei log deve comparire la ricezione dal webhook demo.

Per simulare un consumer irraggiungibile e verificare retry/dead-letter, riavviare l'app con:

```bash
H2_CONSOLE_ENABLED=true \
OUTBOX_WEBHOOK_BASE_URL=http://localhost:9999 \
SPRING_PROFILES_ACTIVE=demo \
./mvnw spring-boot:run
```

Creare nuovamente un ordine. Dopo cinque fallimenti, verificare il contatore e gli eventi dead-letter:

```bash
curl -s -u user:password \
  http://localhost:8081/actuator/metrics/outbox.events.dead-lettered |
jq '.measurements'
```

Con la console H2 abilitata, aprire `http://localhost:8080/h2-console` e interrogare:

```sql
SELECT id, event_type, attempt_count, dead_lettered_at, last_error
FROM outbox_events
WHERE dead_lettered_at IS NOT NULL;
```

### Cleanup

L'H2 del profilo demo e in memoria: arrestare l'app azzera i dati del test. Per fermare lo stack osservabilita:

```bash
docker compose down
```
