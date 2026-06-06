package com.github.swim_developer.dnotam.provider.infrastructure.out.messaging;

import com.github.swim_developer.framework.domain.model.DeliveryResult;
import com.github.swim_developer.framework.infrastructure.out.cache.HandoffCache;
import com.github.swim_developer.framework.provider.application.messaging.AbstractOutboxEventProcessor;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.DnotamEventStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import com.github.swim_developer.dnotam.provider.application.port.in.DeliverEventPort;

@ApplicationScoped
@Slf4j
public class DnotamOutboxEventProcessor extends AbstractOutboxEventProcessor<DnotamStoredEvent> {

    public static final String OUTBOX_EVENT_ADDRESS = "outbox.deliver";

    private final DnotamEventStore eventRepository;
    private final DeliverEventPort deliveryService;

    @Inject
    protected DnotamOutboxEventProcessor(HandoffCache handoffCache,
                                          MeterRegistry registry,
                                          DnotamEventStore eventRepository,
                                          DeliverEventPort deliveryService) {
        super(handoffCache, registry);
        this.eventRepository = eventRepository;
        this.deliveryService = deliveryService;
    }

    protected DnotamOutboxEventProcessor() {
        this(null, null, null, null);
    }

    @ConsumeEvent(OUTBOX_EVENT_ADDRESS)
    @Blocking
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Bulkhead(250)
    @WithSpan("dnotam.provider.outbox.deliver")
    public void onOutboxEvent(String eventId) {
        processWithMetrics(eventId);
    }

    @Override
    protected DeliveryResult deliver(DnotamStoredEvent entity) {
        Span.current().setAttribute("dnotam.scenario",
                entity.getEventScenario() != null ? entity.getEventScenario() : "unknown");
        Span.current().setAttribute("dnotam.airport",
                entity.getAirportHeliport() != null ? entity.getAirportHeliport() : "N/A");

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        Span.current().setAttribute("dnotam.delivery.delivered", result.delivered());
        Span.current().setAttribute("dnotam.delivery.failed", result.failed());

        return result;
    }

    @Override
    protected DnotamStoredEvent findEntityById(String eventId) {
        return eventRepository.findDomainById(eventId);
    }

    @Override
    protected DnotamStoredEvent mergeEntity(DnotamStoredEvent detached) {
        return eventRepository.mergeDomainEntity(detached);
    }

    @Override
    protected Class<DnotamStoredEvent> getEntityClass() {
        return DnotamStoredEvent.class;
    }

    @Override
    protected String getMetricPrefix() {
        return "dnotam";
    }
}
