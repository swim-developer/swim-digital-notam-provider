# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SWIM Digital NOTAM Provider — AISP-role Quarkus service that exposes a DNOTAM subscription REST API (SPEC-170), consumes DNOTAM events from Kafka, validates/assembles AIXM 5.1.1 payloads, and delivers them to subscriber AMQP queues via ActiveMQ Artemis with mTLS.

## Build & Run Commands

```bash
# Build (compiles tests but skips execution)
./mvnw clean package -DskipTests

# Run in dev mode
./mvnw quarkus:dev

# Unit tests only
./mvnw test

# Unit + integration tests (ITs are skipped by default in pom.xml)
./mvnw verify -DskipITs=false

# Run a single test class
./mvnw test -Dtest=DnotamEventDeliveryServiceTest

# Run a single integration test
./mvnw verify -DskipITs=false -Dit.test=DnotamProviderIT

# Local infrastructure (requires podman login registry.redhat.io first)
podman compose up --build -d

# Generate local TLS certificates (one-time)
./certs/generate.sh
```

## Build Rules

- ALWAYS use `-DskipTests` (not `-Dmaven.test.skip=true`) — the former compiles tests to catch compilation errors.
- NEVER use `-Dmaven.test.skip=true`, `mvn compile`, or `mvn compiler:testCompile`.
- NEVER run `mvn verify` without `-DskipITs=false` and claim tests passed — ITs are silently skipped by default.
- Integration tests use Testcontainers — run ONE project at a time (port conflicts).

## Dependencies

This project depends on sibling repos that must be installed into the local Maven repo before building:

1. `swim-developer-root` — `./mvnw install -N -DskipTests`
2. `swim-aixm-model` — `./mvnw clean install -DskipTests`
3. `swim-developer-framework` — `./mvnw clean install -DskipTests`
4. `swim-developer-extensions` — `./mvnw clean install -DskipTests`

Use `make sync` to clone/pull all deps and install them automatically.

## Architecture

Hexagonal architecture (ports & adapters). Base package: `com.github.swim_developer.dnotam.provider`

```
domain/model/              Domain entities (Subscription, DnotamEvent, FailedDelivery)
application/port/in/       Inbound ports (ManageSubscriptionPort, DeliverEventPort, QueryEventPort)
application/port/out/      Outbound ports (SubscriptionStore, EventStore, AixmMessageAssemblerPort)
application/usecase/       Use cases (DnotamSubscriptionUseCase, DnotamEventDeliveryUseCase, DnotamEventQueryUseCase)
infrastructure/in/rest/    JAX-RS REST endpoints (SubscriptionCollectionResource, SubscriptionItemResource, etc.)
infrastructure/in/internal/ Internal Vert.x HTTP server (port 9080) for event injection
infrastructure/in/amqp/    Kafka ingress message handler
infrastructure/out/amqp/   AMQP publisher to Artemis subscriber queues
infrastructure/out/persistence/ PostgreSQL stores (Hibernate Panache)
infrastructure/out/xml/    AIXM JAXB unmarshalling, message assembly, event extraction
infrastructure/out/subscription/ OGC filter parsing, hash-based deduplication, heartbeat, expiry
infrastructure/out/messaging/ Outbox event processor
```

### Framework Wiring

This service extends abstract classes from `swim-developer-framework`. Key extension points:

| Framework Base Class | Provider Implementation |
|---|---|
| `AbstractEventDeliveryService` | `DnotamEventDeliveryUseCase` |
| `AbstractAmqpPublisher` | `DnotamAmqpPublisher` |
| `AbstractProviderSubscriptionService` | `DnotamSubscriptionUseCase` |
| `SwimEventExtractor` SPI | `DnotamEventExtractor` |
| `AixmMessageAssemblerPort` SPI | `DnotamAixmMessageAssembler` |
| `SwimFailedDeliveryStorePort` SPI | `DnotamFailedDeliveryStore` |
| `SubscriptionExpiryStrategy` SPI | `DnotamExpiryStrategy` |

### Queue Provisioning

| Environment | Implementation | Activation |
|---|---|---|
| Dev/test | `ArtemisJmxQueueProvisioner` (Jolokia REST) | `@UnlessBuildProfile("prod")` |
| Production | `KubernetesQueueProvisioner` (K8s Secrets + AMQ Operator) | `@IfBuildProfile("prod")` |

### Event Flow

Kafka → `DnotamIngressMessageHandler` → `DnotamEventExtractor` (metadata) → `DnotamEventDeliveryUseCase` (fan-out) → `DnotamAixmMessageAssembler` → `DnotamAmqpPublisher` → Artemis subscriber queues

## REST API

| Method | Path | Auth |
|---|---|---|
| `POST` | `/swim/v1/subscriptions` | OIDC |
| `GET` | `/swim/v1/subscriptions` | OIDC |
| `GET/PUT/DELETE` | `/swim/v1/subscriptions/{id}` | OIDC |
| `GET` | `/swim/v1/topics` | OIDC |
| `GET` | `/swim/v1/features` | OIDC |
| `GET` | `/swim/v1/subscriptions/ping` | public |

Internal API (port 9080): `/internal/v1/trigger`, `/internal/v1/validate`, `/internal/v1/subscriptions/summary`, `/internal/v1/status`

## Testing

- Unit tests: `src/test/java/.../unit/` — Mockito, Panache mock, RestAssured, AssertJ
- Integration tests: `src/test/java/.../integration/DnotamProviderIT.java` — Testcontainers (PostgreSQL, Artemis)
- ArchUnit tests enforce hexagonal dependency rules
- For HTTP/API tests: use **RestAssured** for requests, **AssertJ** for assertions
- Sample AIXM payloads: `src/test/resources/messages/`
- Postman collection: `src/test/postman/`

## Code Standards

- Use `jq` for JSON in shell — never Python/Node
- NEVER change design patterns or architectural choices without explicit approval

## Security

- mTLS for external AMQP consumers (X.509 certificates)
- OIDC/JWT via Keycloak for REST API (`swim` realm)
- Artemis JAAS validates Keycloak JWT via `BearerTokenLoginModule`
- TLS 1.3 (SPEC-170 SWIM-TIYP-0008: TLS 1.2 deprecated)

## Local Dev Environment

Requires `/etc/hosts` entry: `127.0.0.1  keycloak.swim.lab`

Services via `podman compose up`:
- Artemis: ports 5671 (AMQPS/mTLS), 5672 (AMQP), 8161 (console, admin/admin)
- PostgreSQL: 5432
- Kafka (KRaft): 9092
- AKHQ (Kafka UI): 9090
- Keycloak: 8543 (admin/password)

Keycloak test users: see `compose.yml` or `application.properties` for current credentials

## Read-Only Directories

- `nav-portugal/` — reference only, never modify
