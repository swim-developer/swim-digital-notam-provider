package com.github.swim_developer.unit;

import com.github.swim_developer.framework.application.port.out.FailedDeliveryStore;
import com.github.swim_developer.framework.domain.model.DeliveryResult;
import com.github.swim_developer.framework.domain.model.QualityOfService;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.domain.model.FailedDelivery;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.amqp.DnotamAmqpPublisher;
import com.github.swim_developer.dnotam.provider.application.usecase.DnotamEventDeliveryUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TestNameLoggerExtension.class})
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DnotamEventDeliveryServiceTest {

    @InjectMocks
    private DnotamEventDeliveryUseCase deliveryService;

    @Mock
    private MongoSubscriptionStore subscriptionRepository;

    @Mock
    private DnotamAmqpPublisher amqpPublisher;

    @Mock
    @SuppressWarnings("unchecked")
    private FailedDeliveryStore<FailedDelivery> failedDeliveryRepository;

    @Spy
    private MeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void deliversToMatchingSubscription() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildSubscription(subId, List.of("RWY.CLS"), List.of("EHAM"));
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(sub));

        DnotamStoredEvent entity = buildEntity("EVT-001", "RWY.CLS", "EHAM", null);

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.delivered()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(amqpPublisher).publishToQueue(sub.getQueue(), "<aixm/>",
                QualityOfService.AT_LEAST_ONCE, subId);
    }

    @Test
    void skipsNonMatchingSubscription() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildSubscription(subId, List.of("SAA.ACT"), List.of("LFPG"));
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(sub));

        DnotamStoredEvent entity = buildEntity("EVT-002", "RWY.CLS", "EHAM", null);

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        assertThat(result.matched()).isZero();
        assertThat(result.delivered()).isZero();
        verifyNoInteractions(amqpPublisher);
    }

    @Test
    void handlesPublishFailureGracefully() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildSubscription(subId, List.of(), List.of());
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(sub));
        doThrow(new RuntimeException("Broker offline")).when(amqpPublisher)
                .publishToQueue(anyString(), anyString(), any(), any());

        DnotamStoredEvent entity = buildEntity("EVT-003", "RWY.CLS", "EHAM", null);

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.delivered()).isZero();
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    void deliversToMultipleMatchingSubscriptions() {
        UUID subId1 = UUID.randomUUID();
        UUID subId2 = UUID.randomUUID();
        Subscription sub1 = buildSubscription(subId1, List.of("RWY.CLS"), List.of());
        Subscription sub2 = buildSubscription(subId2, List.of(), List.of("EHAM"));
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(sub1, sub2));

        DnotamStoredEvent entity = buildEntity("EVT-004", "RWY.CLS", "EHAM", null);

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        assertThat(result.matched()).isEqualTo(2);
        assertThat(result.delivered()).isEqualTo(2);
        verify(amqpPublisher, times(2)).publishToQueue(anyString(), anyString(), any(), any());
    }

    @Test
    void noActiveSubscriptionsReturnsZeroCounts() {
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of());

        DnotamStoredEvent entity = buildEntity("EVT-005", "RWY.CLS", "EHAM", null);

        DeliveryResult result = deliveryService.deliverToMatchingSubscriptions(entity);

        assertThat(result.matched()).isZero();
        assertThat(result.delivered()).isZero();
        assertThat(result.failed()).isZero();
    }

    @Test
    void incrementsCounterOnSuccessfulDelivery() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildSubscription(subId, List.of(), List.of());
        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(sub));

        deliveryService.deliverToMatchingSubscriptions(buildEntity("EVT-006", "RWY.CLS", "EHAM", null));

        assertThat(registry.counter("dnotam_events_delivered_total").count()).isEqualTo(1.0);
    }

    private Subscription buildSubscription(UUID id, List<String> scenarios, List<String> airports) {
        return Subscription.builder()
                .subscriptionId(id)
                .queue("DNOTAM-user1-" + id)
                .status(SubscriptionStatus.ACTIVE)
                .qos(QualityOfService.AT_LEAST_ONCE)
                .eventScenario(scenarios)
                .airportHeliport(airports)
                .build();
    }

    private DnotamStoredEvent buildEntity(String id, String scenario, String airport, String airspace) {
        return DnotamStoredEvent.builder()
                .eventId(id)
                .eventScenario(scenario)
                .airportHeliport(airport)
                .airspace(airspace)
                .validFrom(Instant.parse("2025-01-01T00:00:00Z"))
                .validTo(Instant.parse("2025-01-02T00:00:00Z"))
                .aixmMessage("<aixm/>")
                .build();
    }
}
