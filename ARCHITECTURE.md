# swim-dnotam-provider — Architecture

> Diagrams use [Mermaid](https://mermaid.js.org) and render natively on GitHub.

**Role**: AISP (Aeronautical Information Service Provider) — exposes a DNOTAM subscription REST API, receives DNOTAM events from Kafka, validates and assembles AIXM 5.1.1 payloads, and delivers them to subscriber AMQP queues.

---

## 1. System Context (C4 Level 1)

```mermaid
C4Context
    title System Context — swim-dnotam-provider

    Person(subscriber, "ANSP / Consumer", "Subscribes to DNOTAM topics and receives events via AMQP")
    Person(operator, "AISP Operator", "Configures the provider and monitors service health")

    System(provider, "swim-dnotam-provider", "DNOTAM Provider: subscription management REST API + AMQP event delivery to subscribers")

    System_Ext(kafka, "Apache Kafka", "Source of DNOTAM events (upstream ingestion pipeline)")
    System_Ext(broker, "AMQP Broker", "ActiveMQ Artemis — AMQP 1.0 / mTLS — subscriber queues provisioned here")
    System_Ext(postgres, "PostgreSQL", "Subscription and event persistence")

    Rel(subscriber, provider, "Creates, updates and deletes subscriptions", "REST / HTTPS / mTLS")
    Rel(provider, kafka, "Consumes incoming DNOTAM events")
    Rel(provider, broker, "Publishes events to per-subscriber queues", "AMQP 1.0 / mTLS")
    Rel(provider, postgres, "Persists subscriptions and events")
    Rel(operator, provider, "Monitors and manages", "REST / HTTPS")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

---

## 2. Container Diagram (C4 Level 2)

```mermaid
C4Container
    title Container Diagram — swim-dnotam-provider

    Person(subscriber, "ANSP / Consumer")
    Person(operator, "AISP Operator")

    System_Ext(kafka, "Apache Kafka", "Incoming DNOTAM event source")
    System_Ext(broker, "AMQP Broker", "ActiveMQ Artemis — AMQP 1.0 / mTLS")

    System_Boundary(sys, "swim-dnotam-provider") {
        Container(app, "swim-dnotam-provider", "Quarkus / Java 21", "Subscription REST API, AIXM 5.1.1 event delivery, heartbeat, expiry")
        ContainerDb(postgres, "PostgreSQL", "Relational DB", "Subscriptions and DNOTAM events")
    }

    Rel(subscriber, app, "Manages subscriptions", "REST / HTTPS / mTLS")
    Rel(operator, app, "Monitors service", "REST / HTTPS")
    Rel(app, kafka, "Consumes incoming DNOTAM events")
    Rel(app, broker, "Publishes to subscriber queues", "AMQP 1.0 / mTLS")
    Rel(app, postgres, "Persists subscriptions and events")
```

---

## 3. Component Diagram (C4 Level 3)

```mermaid
C4Component
    title Component Diagram — swim-dnotam-provider

    System_Ext(kafka, "Apache Kafka")
    System_Ext(broker, "AMQP Broker")
    System_Ext(postgres, "PostgreSQL")

    Container_Boundary(provider, "swim-dnotam-provider") {
        Component(subColRes, "SubscriptionCollectionResource", "JAX-RS", "REST — subscription creation and listing")
        Component(subItemRes, "SubscriptionItemResource", "JAX-RS", "REST — subscription update and deletion")
        Component(featureRes, "FeatureResource", "JAX-RS", "WFS GetFeature endpoint — queries persisted DNOTAM events")
        Component(topicRes, "TopicResource", "JAX-RS", "REST — topic listing")

        Component(ingressHandler, "DnotamIngressMessageHandler", "SmallRye Messaging", "Kafka consumer — receives events, delegates to delivery use case")

        Component(subUC, "DnotamSubscriptionUseCase", "CDI", "Subscriber registration, queue provisioning, update, deletion")
        Component(deliveryUC, "DnotamEventDeliveryUseCase", "CDI", "Fan-out: delivers event to all active subscriber AMQP queues")
        Component(queryUC, "DnotamEventQueryUseCase", "CDI", "WFS query over persisted events — OGC filter parsing")

        Component(amqpPublisher, "DnotamAmqpPublisher", "Qpid JMS / SwimAmqpPublisherPort SPI", "Sends AMQP messages to individual subscriber queues")
        Component(heartbeat, "DnotamHeartbeatPublisher", "Quartz", "Publishes heartbeat messages to subscriber queues")
        Component(expiry, "DnotamExpiryStrategy", "Quartz / SubscriptionExpiryStrategy SPI", "Marks subscriptions as expired when TTL is exceeded")

        Component(assembler, "DnotamAixmMessageAssembler", "CDI / AixmMessageAssemblerPort SPI", "Assembles AIXM 5.1.1 Basic Message for delivery")
        Component(extractor, "DnotamEventExtractor", "CDI / SwimEventExtractor SPI", "Extracts event type and metadata from AIXM payload")
        Component(jaxbPool, "DnotamJaxbUnmarshallerPool", "JAXB", "Thread-safe pool of JAXB unmarshallers for AIXM 5.1.1 XML")

        Component(mongoSub, "MongoSubscriptionStore", "Panache / SubscriptionStore port/out", "Subscriber persistence")
        Component(evtStore, "DnotamEventStore", "Panache / EventStore port/out", "DNOTAM event persistence")
        Component(failedStore, "DnotamFailedDeliveryStore", "Panache / SwimFailedDeliveryStorePort SPI", "Failed delivery persistence for retry")
    }

    Rel(subColRes, subUC, "calls via", "ManageSubscriptionPort")
    Rel(subItemRes, subUC, "calls via", "ManageSubscriptionPort")
    Rel(featureRes, queryUC, "calls via", "QueryEventPort")
    Rel(ingressHandler, extractor, "extracts metadata with")
    Rel(ingressHandler, deliveryUC, "delegates to via", "DeliverEventPort")
    Rel(deliveryUC, assembler, "assembles payload with")
    Rel(deliveryUC, amqpPublisher, "publishes via", "SwimAmqpPublisherPort")
    Rel(deliveryUC, mongoSub, "reads active subscribers via", "SubscriptionStore port")
    Rel(deliveryUC, evtStore, "persists events via", "EventStore port")
    Rel(deliveryUC, failedStore, "stores failures via", "SwimFailedDeliveryStorePort")
    Rel(heartbeat, amqpPublisher, "sends heartbeats via", "SwimAmqpPublisherPort")
    Rel(expiry, subUC, "expires subscriptions")

    Rel(ingressHandler, kafka, "consumes from")
    Rel(amqpPublisher, broker, "publishes to", "AMQP 1.0 / mTLS")
    Rel(mongoSub, postgres, "persists to")
    Rel(evtStore, postgres, "persists to")
    Rel(failedStore, postgres, "persists to")
```

---

## 4. Event Delivery — Sequence

```mermaid
sequenceDiagram
    autonumber
    participant Kafka as Apache Kafka
    participant Handler as DnotamIngressMessageHandler
    participant Extractor as DnotamEventExtractor
    participant UC as DnotamEventDeliveryUseCase
    participant Assembler as DnotamAixmMessageAssembler
    participant Store as MongoSubscriptionStore
    participant Publisher as DnotamAmqpPublisher
    participant Broker as AMQP Broker

    Kafka->>Handler: DNOTAM event message
    Handler->>Extractor: extract(payload)
    Extractor-->>Handler: event type + metadata
    Handler->>UC: deliver(event) via DeliverEventPort
    UC->>Store: load active subscriptions
    Store-->>UC: subscriber list
    loop for each active subscriber
        UC->>Assembler: assemble AIXM Basic Message
        Assembler-->>UC: AMQP message payload
        UC->>Publisher: publish(queue, message)
        Publisher->>Broker: send to subscriber queue (AMQP / mTLS)
    end
    UC-->>Handler: delivery complete

    Note over Publisher,Broker: On delivery failure: stored in DnotamFailedDeliveryStore<br/>for retry by AbstractFailedDeliveryRecoveryScheduler (framework).
```
