package com.github.swim_developer.dnotam.provider.infrastructure.out.persistence;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.domain.model.FailedDelivery;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity.DnotamEventJpaEntity;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity.FailedDeliveryJpaEntity;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity.SubscriptionJpaEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DnotamProviderPersistenceMapper {

    public SubscriptionJpaEntity toJpa(Subscription domain) {
        return SubscriptionJpaEntity.builder()
                .subscriptionId(domain.getSubscriptionId())
                .topic(domain.getTopic())
                .eventScenario(domain.getEventScenario())
                .airportHeliport(domain.getAirportHeliport())
                .airspace(domain.getAirspace())
                .eventSeries(domain.getEventSeries())
                .publisher(domain.getPublisher())
                .provider(domain.getProvider())
                .queue(domain.getQueue())
                .status(domain.getStatus())
                .qos(domain.getQos())
                .durable(domain.getDurable())
                .userId(domain.getUserId())
                .subscriptionHash(domain.getSubscriptionHash())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .subscriptionEnd(domain.getSubscriptionEnd())
                .description(domain.getDescription())
                .comment(domain.getComment())
                .build();
    }

    public Subscription toDomain(SubscriptionJpaEntity jpa) {
        return Subscription.builder()
                .subscriptionId(jpa.getSubscriptionId())
                .topic(jpa.getTopic())
                .eventScenario(jpa.getEventScenario())
                .airportHeliport(jpa.getAirportHeliport())
                .airspace(jpa.getAirspace())
                .eventSeries(jpa.getEventSeries())
                .publisher(jpa.getPublisher())
                .provider(jpa.getProvider())
                .queue(jpa.getQueue())
                .status(jpa.getStatus())
                .qos(jpa.getQos())
                .durable(jpa.getDurable())
                .userId(jpa.getUserId())
                .subscriptionHash(jpa.getSubscriptionHash())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .subscriptionEnd(jpa.getSubscriptionEnd())
                .description(jpa.getDescription())
                .comment(jpa.getComment())
                .build();
    }

    public DnotamEventJpaEntity toJpa(DnotamStoredEvent domain) {
        return DnotamEventJpaEntity.builder()
                .eventId(domain.getEventId())
                .eventScenario(domain.getEventScenario())
                .airportHeliport(domain.getAirportHeliport())
                .airspace(domain.getAirspace())
                .eventSeries(domain.getEventSeries())
                .publisher(domain.getPublisher())
                .provider(domain.getProvider())
                .validFrom(domain.getValidFrom())
                .validTo(domain.getValidTo())
                .status(domain.getStatus())
                .receivedAt(domain.getReceivedAt())
                .processedAt(domain.getProcessedAt())
                .deliveredCount(domain.getDeliveredCount())
                .retryCount(domain.getRetryCount())
                .aixmMessage(domain.getAixmMessage())
                .build();
    }

    public DnotamStoredEvent toDomain(DnotamEventJpaEntity jpa) {
        return DnotamStoredEvent.builder()
                .eventId(jpa.getEventId())
                .eventScenario(jpa.getEventScenario())
                .airportHeliport(jpa.getAirportHeliport())
                .airspace(jpa.getAirspace())
                .eventSeries(jpa.getEventSeries())
                .publisher(jpa.getPublisher())
                .provider(jpa.getProvider())
                .validFrom(jpa.getValidFrom())
                .validTo(jpa.getValidTo())
                .status(jpa.getStatus())
                .receivedAt(jpa.getReceivedAt())
                .processedAt(jpa.getProcessedAt())
                .deliveredCount(jpa.getDeliveredCount())
                .retryCount(jpa.getRetryCount())
                .aixmMessage(jpa.getAixmMessage())
                .build();
    }

    public FailedDeliveryJpaEntity toJpa(FailedDelivery domain) {
        return FailedDeliveryJpaEntity.builder()
                .id(domain.getId())
                .eventId(domain.getEventId())
                .subscriptionId(domain.getSubscriptionId())
                .queue(domain.getQueue())
                .errorMessage(domain.getErrorMessage())
                .retryCount(domain.getRetryCount())
                .resolved(domain.isResolved())
                .createdAt(domain.getCreatedAt())
                .resolvedAt(domain.getResolvedAt())
                .build();
    }

    public FailedDelivery toDomain(FailedDeliveryJpaEntity jpa) {
        return FailedDelivery.builder()
                .id(jpa.getId())
                .eventId(jpa.getEventId())
                .subscriptionId(jpa.getSubscriptionId())
                .queue(jpa.getQueue())
                .errorMessage(jpa.getErrorMessage())
                .retryCount(jpa.getRetryCount())
                .resolved(jpa.isResolved())
                .createdAt(jpa.getCreatedAt())
                .resolvedAt(jpa.getResolvedAt())
                .build();
    }
}
