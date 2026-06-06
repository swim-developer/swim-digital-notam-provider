# swim-digital-notam-provider — Knowledge Base


## What This Is

**AISP (Aeronautical Information Service Provider) role.** Exposes SWIM-compliant REST API for subscription management, topic discovery, and WFS feature requests. Consumes DNOTAM events from Kafka and publishes them to AMQP queues for downstream ANSP consumers.

~63 classes. 120 unit + 17 integration tests.

## REST API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/swim/v1/subscriptions` | Create subscription (default: PAUSED) |
| GET | `/swim/v1/subscriptions` | List subscriptions |
| GET | `/swim/v1/subscriptions/{id}` | Get subscription |
| PUT | `/swim/v1/subscriptions/{id}` | Update (ACTIVE/PAUSED) |
| DELETE | `/swim/v1/subscriptions/{id}` | Delete subscription |
| GET | `/swim/v1/topics` | List topics |
| GET | `/swim/v1/topics/{id}` | Get topic |
| GET | `/swim/v1/features` | WFS GetFeature (AIXM + OGC filter) |

## Architecture

```
com.github.swim_developer.dnotam.provider
├── domain/model/        Subscription, Topic
├── application/usecase/ Subscription and event use cases
├── application/service/ Application services
└── infrastructure/
    ├── in/rest/         REST endpoints
    ├── in/scheduling/   Heartbeat scheduler, expiry scheduler
    └── out/
        ├── persistence/ PostgreSQL (Hibernate Panache)
        ├── amqp/        Artemis AMQP publishing
        ├── kafka/       Kafka consumers (6 DNOTAM topics)
        └── artemis/     JMX queue management
```

## Framework Wiring

| Framework Class | Usage |
|-----------------|--------|
| `AbstractEventDeliveryService` | Load subscriptions → filter by OGC → publish to AMQP |
| `AbstractAmqpPublisher` | Publishes to Artemis queues |
| `AbstractProviderSubscriptionService` | Subscription lifecycle (create/activate/pause/delete) |
| `PerSubscriptionHeartbeatScheduler` | JSON heartbeat to `{queue}.heartbeat` every ACTIVE/PAUSED subscription |
| `SwimSubscriptionExpiryScheduler` | Auto-purge expired subscriptions |
| `AbstractOutboxEventProcessor` | Provider-side outbox |
| `DatabaseReadinessCheck` | Auto-inherited PostgreSQL health check |

## Queue Provisioning (Dev vs Prod)

| Environment | Implementation | Activation |
|-------------|----------------|------------|
| Dev/test | `ArtemisJmxQueueProvisioner` — Jolokia REST | `@UnlessBuildProfile("prod")` |
| Production | `KubernetesQueueProvisioner` — K8s Secrets + AMQ Operator | `@IfBuildProfile("prod")` |

Framework injects the correct bean automatically. Developers test with real broker ACLs locally without Kubernetes.

## PostgreSQL

- DB: `swim-dnotam`
- Table: `subscriptions` (state, queue name, OGC filters, expiry, `subscriptionEnd`)

## Security

- mTLS: X.509 certificates for external consumers
- AMQP JWT: Keycloak `BearerTokenLoginModule` on Artemis
- OAuth 2.0: Keycloak for REST API
- `JwtRoleValidator`: role-based access per AMQP queue

## Build & Run

```bash
# Prerequisites: swim-developer-framework installed
cd ../swim-developer-framework && mvn clean install -DskipTests

# Build
./mvnw clean package -DskipTests

# Dev mode (auto-provisions PostgreSQL, Kafka via Dev Services; Artemis via compose)
quarkus dev

# Integration tests
./mvnw verify -DskipITs=false
```

Local infra: `podman compose up -d` (requires a compose.yml with Kafka, MongoDB/PostgreSQL, Artemis — see repo root)`

## Observability

- Tracing: custom spans with `dnotam.eventId`, `dnotam.scenario`, `dnotam.airport` attributes
- Metrics: `swim_heartbeats_sent_total`, `swim_subscriptions_terminated_total`, `swim_subscriptions_purged_total`
- Logging: JSON structured (Loki-friendly)
