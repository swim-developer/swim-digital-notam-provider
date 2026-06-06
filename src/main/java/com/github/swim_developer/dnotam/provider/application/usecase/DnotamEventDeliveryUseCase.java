package com.github.swim_developer.dnotam.provider.application.usecase;

import com.github.swim_developer.dnotam.provider.application.port.in.DeliverEventPort;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.framework.domain.model.QualityOfService;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.provider.application.subscription.AbstractEventDeliveryService;
import com.github.swim_developer.framework.application.port.out.FailedDeliveryStore;
import com.github.swim_developer.framework.domain.model.SwimFailedDelivery;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.domain.model.FailedDelivery;
import com.github.swim_developer.dnotam.provider.application.port.out.SubscriptionStore;
import com.github.swim_developer.framework.application.port.out.SwimAmqpPublisherPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class DnotamEventDeliveryUseCase extends AbstractEventDeliveryService<DnotamStoredEvent, DnotamEvent, Subscription>
        implements DeliverEventPort {

    private final SubscriptionStore subscriptionRepository;
    private final SwimAmqpPublisherPort amqpPublisher;
    private final FailedDeliveryStore<FailedDelivery> failedDeliveryRepository;
    private final MeterRegistry registry;

    @Inject
    public DnotamEventDeliveryUseCase(SubscriptionStore subscriptionRepository,
                                       SwimAmqpPublisherPort amqpPublisher,
                                       FailedDeliveryStore<FailedDelivery> failedDeliveryRepository,
                                       MeterRegistry registry) {
        this.subscriptionRepository = subscriptionRepository;
        this.amqpPublisher = amqpPublisher;
        this.failedDeliveryRepository = failedDeliveryRepository;
        this.registry = registry;
    }

    @Override
    protected DnotamEvent toFilterableModel(DnotamStoredEvent entity) {
        return new DnotamEvent(
                entity.getEventId(),
                entity.getEventScenario(),
                entity.getAirportHeliport(),
                entity.getAirspace(),
                entity.getEventSeries(),
                entity.getPublisher(),
                entity.getProvider(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getAixmMessage()
        );
    }

    @Override
    protected String extractPayload(DnotamStoredEvent entity) {
        return entity.getAixmMessage();
    }

    @Override
    protected String extractEventId(DnotamStoredEvent entity) {
        return entity.getEventId();
    }

    @Override
    protected List<Subscription> loadActiveSubscriptions() {
        return subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
    }

    @Override
    protected void publishToQueue(String queue, String payload, QualityOfService qos, UUID subscriptionId) {
        amqpPublisher.publishToQueue(queue, payload, qos, subscriptionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Optional<FailedDeliveryStore<SwimFailedDelivery>> getFailedDeliveryStore() {
        return Optional.of((FailedDeliveryStore<SwimFailedDelivery>) (FailedDeliveryStore<?>) failedDeliveryRepository);
    }

    @Override
    protected void onDeliverySuccess(DnotamStoredEvent entity, Subscription subscription) {
        Counter.builder("dnotam_events_delivered_total")
                .description("Total DNOTAM events delivered to AMQP queues")
                .register(registry)
                .increment();
    }

    @Override
    protected void onDeliveryFailure(DnotamStoredEvent entity, Subscription subscription, Exception e) {
        Counter.builder("dnotam_events_delivery_failed_total")
                .description("Total DNOTAM events that failed delivery per subscriber")
                .register(registry)
                .increment();
    }
}
