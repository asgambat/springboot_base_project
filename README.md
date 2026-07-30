# Spring Boot Microservice Base Project

Template Spring Boot per sperimentare e riusare pratiche comuni nei microservizi: API REST, validazione, sicurezza, persistence con JPA/Flyway, cache, chiamate HTTP, resilienza, osservabilita, idempotenza dei pagamenti e transactional outbox.

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

- `demo` (default): H2 in memoria e Basic Auth con credenziali configurabili tramite `DEMO_USER` e `DEMO_PASSWORD`.
- `production`: OAuth2 Resource Server con JWT, CORS esplicito e accesso Actuator protetto dallo scope `actuator.read`.
- `dev`: abilita DevTools, logging Hibernate dettagliato e sampling tracing al 100%.

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
| `OUTBOX_WEBHOOK_ENABLED` | Abilita il publisher webhook dell'outbox | Default `false`. |
| `OUTBOX_WEBHOOK_BASE_URL` | Base URL del consumer webhook | Richiesta quando il webhook e abilitato. |

Le opzioni Hikari, Hibernate e del client HTTP sono configurabili con le rispettive variabili esposte nel file YAML.

## Sicurezza

Gli endpoint `/payments/**` e `/api/secured/**` richiedono HTTP Basic. Il template include un utente dimostrativo `user` / `password` esclusivamente per sviluppo e test; non usare queste credenziali in alcun ambiente condiviso.

Prima della produzione:

1. sostituire l'utente in memoria con un identity provider, ad esempio OAuth2 Resource Server e JWT;
2. usare HTTPS terminato in modo affidabile;
3. caricare tutte le credenziali da un secret store;
4. restringere l'accesso alla porta management con rete/firewall;
5. mantenere disabilitati logging HTTP verboso e console H2.

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

Arrestare e rimuovere i container con `docker compose down`.

## Contratto API

Il contratto OpenAPI e generato automaticamente da Springdoc durante l'avvio:

- JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Qualita e container

Il build standard applica Maven Enforcer e richiede Java 25. I quality gate aggiuntivi sono attivabili in CI o localmente:

```powershell
.\mvnw.cmd verify -Pquality
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

La suite include anche test espliciti per apertura del circuit breaker, saturazione del bulkhead e pubblicazione webhook end-to-end. I test PostgreSQL Testcontainers sono taggati `testcontainers` ed esclusi dalla suite locale standard, per non richiedere Docker in ogni esecuzione. Per eseguirli dove Docker e disponibile:

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