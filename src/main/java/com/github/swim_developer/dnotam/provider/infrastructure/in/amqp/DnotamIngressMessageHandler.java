package com.github.swim_developer.dnotam.provider.infrastructure.in.amqp;

import aero.aixm.message.AIXMBasicMessageType;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamEventExtractor;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamJaxbUnmarshallerPool;
import com.github.swim_developer.dnotam.provider.infrastructure.out.messaging.DnotamOutboxEventProcessor;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.DnotamEventStore;
import com.github.swim_developer.framework.application.port.in.SwimIngressHandler;
import com.github.swim_developer.framework.domain.exception.XmlValidationException;
import com.github.swim_developer.framework.domain.model.EventStatus;
import com.github.swim_developer.framework.infrastructure.out.cache.HandoffCache;
import com.github.swim_developer.framework.provider.application.messaging.AfterCommitEventDispatcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class DnotamIngressMessageHandler implements SwimIngressHandler {

    private static final String FAILED_STATUS = "failed";

    private final DnotamEventStore eventRepository;
    private final DnotamEventExtractor eventExtractor;
    private final DnotamJaxbUnmarshallerPool jaxbPool;
    private final HandoffCache handoffCache;
    private final Vertx vertx;
    private final MeterRegistry registry;
    private final TransactionSynchronizationRegistry txSyncRegistry;

    @Inject
    public DnotamIngressMessageHandler(DnotamEventStore eventRepository,
                                DnotamEventExtractor eventExtractor,
                                DnotamJaxbUnmarshallerPool jaxbPool,
                                HandoffCache handoffCache,
                                Vertx vertx,
                                MeterRegistry registry,
                                TransactionSynchronizationRegistry txSyncRegistry) {
        this.eventRepository = eventRepository;
        this.eventExtractor = eventExtractor;
        this.jaxbPool = jaxbPool;
        this.handoffCache = handoffCache;
        this.vertx = vertx;
        this.registry = registry;
        this.txSyncRegistry = txSyncRegistry;
    }

    @Override
    @Transactional
    @Retry(maxRetries = 2, delay = 500)
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @Bulkhead(value = 100)
    @WithSpan("dnotam.provider.process")
    public void processEvent(String aixmMessage) {
        Timer.Sample timerSample = Timer.start(registry);
        log.debug("Processing DNOTAM event - Phase 1: Validate & Persist");

        AIXMBasicMessageType parsed;
        try {
            parsed = jaxbPool.unmarshalAndValidate(aixmMessage);
        } catch (XmlValidationException e) {
            Span.current().setAttribute("dnotam.validation", FAILED_STATUS);
            log.warn("AIXM JAXB validation failed — event rejected: {}", e.getMessage());
            incrementFailedCounter("jaxb_validation_failed");
            return;
        }

        Optional<DnotamEvent> extracted = eventExtractor.extract(parsed).stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (extracted.isEmpty()) {
            Span.current().setAttribute("dnotam.extraction", FAILED_STATUS);
            log.warn("Failed to extract DNOTAM event from message");
            incrementFailedCounter("extraction_failed");
            return;
        }

        DnotamEvent event = withRawXml(extracted.get(), aixmMessage);

        String scenario = normalizeScenario(event.eventScenario());
        incrementReceivedCounter(scenario);

        Span.current().setAttribute("dnotam.eventId", event.eventId());
        Span.current().setAttribute("dnotam.scenario", scenario);
        Span.current().setAttribute("dnotam.airport", event.airportHeliport() != null ? event.airportHeliport() : "N/A");
        Span.current().setAttribute("dnotam.airspace", event.airspace() != null ? event.airspace() : "N/A");

        log.debug("Extracted event - ID: {}, Scenario: {}, Airport: {}, Airspace: {}",
                event.eventId(), event.eventScenario(), event.airportHeliport(), event.airspace());

        DnotamStoredEvent entity = persistWithStatusReceived(event);
        if (entity == null) {
            Span.current().setAttribute("dnotam.persist", FAILED_STATUS);
            return;
        }

        Span.current().setAttribute("dnotam.persist", "success");
        dispatchForAsyncDelivery(entity);

        timerSample.stop(Timer.builder("dnotam_event_processing_duration")
                .description("Time to process and persist a DNOTAM event")
                .tag("scenario", scenario)
                .register(registry));

        log.info("Event persisted and dispatched - EventId: {}, Status: RECEIVED", event.eventId());
    }

    private DnotamEvent withRawXml(DnotamEvent event, String rawXml) {
        return new DnotamEvent(
                event.eventId(), event.eventScenario(), event.airportHeliport(),
                event.airspace(), event.eventSeries(), event.publisher(),
                event.provider(), event.validFrom(), event.validTo(), rawXml
        );
    }

    private DnotamStoredEvent persistWithStatusReceived(DnotamEvent event) {
        try {
            DnotamStoredEvent entity = eventRepository.findDomainById(event.eventId());

            if (entity != null) {
                log.debug("Event {} already exists, updating in place", event.eventId());
                entity.setEventScenario(event.eventScenario());
                entity.setAirportHeliport(event.airportHeliport());
                entity.setAirspace(event.airspace());
                entity.setEventSeries(event.eventSeries());
                entity.setPublisher(event.publisher());
                entity.setProvider(event.provider());
                entity.setValidFrom(event.validFrom());
                entity.setValidTo(event.validTo());
                entity.setAixmMessage(event.aixmMessage());
                entity.setStatus(EventStatus.RECEIVED);
                entity.setDeliveredCount(0);
                entity.setRetryCount(0);
                entity.setProcessedAt(null);
                eventRepository.update(entity);
            } else {
                entity = DnotamStoredEvent.builder()
                        .eventId(event.eventId())
                        .eventScenario(event.eventScenario())
                        .airportHeliport(event.airportHeliport())
                        .airspace(event.airspace())
                        .eventSeries(event.eventSeries())
                        .publisher(event.publisher())
                        .provider(event.provider())
                        .validFrom(event.validFrom())
                        .validTo(event.validTo())
                        .aixmMessage(event.aixmMessage())
                        .status(EventStatus.RECEIVED)
                        .build();
                eventRepository.persist(entity);
            }

            incrementPersistedCounter();
            log.debug("Persisted DNOTAM event with RECEIVED status: {}", event.eventId());
            return entity;

        } catch (Exception e) {
            log.error("Failed to persist DNOTAM event: {}", event.eventId(), e);
            incrementFailedCounter("persistence_failed");
            return null;
        }
    }

    private void dispatchForAsyncDelivery(DnotamStoredEvent entity) {
        String eventId = entity.getEventId();

        txSyncRegistry.registerInterposedSynchronization(
                new AfterCommitEventDispatcher(eventId, entity, handoffCache, vertx,
                        DnotamOutboxEventProcessor.OUTBOX_EVENT_ADDRESS));

        log.debug("Event dispatch scheduled for after commit - EventId: {}", eventId);
    }

    private void incrementReceivedCounter(String scenario) {
        Counter.builder("dnotam_events_received_total")
                .description("Total DNOTAM events received from Kafka")
                .tag("scenario", scenario)
                .register(registry)
                .increment();
    }

    private void incrementPersistedCounter() {
        Counter.builder("dnotam_events_persisted_total")
                .description("Total DNOTAM events persisted to database with RECEIVED status")
                .register(registry)
                .increment();
    }

    private void incrementFailedCounter(String reason) {
        Counter.builder("dnotam_events_failed_total")
                .description("Total DNOTAM events that failed processing")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    private String normalizeScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            return "unknown";
        }
        String upper = scenario.toUpperCase();
        if (upper.startsWith("RWY.CLS") || upper.contains("CLOSURE")) {
            return "closure";
        } else if (upper.startsWith("RWY.LIM") || upper.contains("RESTRICTION")) {
            return "restriction";
        } else if (upper.startsWith("SFC.CON") || upper.contains("SURFACE")) {
            return "surface_condition";
        } else if (upper.startsWith("SAA.ACT") || upper.contains("AIRSPACE")) {
            return "airspace";
        } else if (upper.contains("NAV") || upper.contains("OBS") || upper.contains("HAZARD")) {
            return "hazards_navaids";
        }
        return "others";
    }
}
