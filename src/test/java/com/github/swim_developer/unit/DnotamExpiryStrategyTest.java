package com.github.swim_developer.unit;

import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.framework.domain.model.SubscriptionExpiry;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.DnotamExpiryStrategy;
import com.github.swim_developer.dnotam.provider.application.usecase.DnotamSubscriptionUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TestNameLoggerExtension.class})
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DnotamExpiryStrategyTest {

    @InjectMocks
    private DnotamExpiryStrategy strategy;

    @Mock
    private MongoSubscriptionStore subscriptionRepository;

    @Mock
    private DnotamSubscriptionUseCase subscriptionService;

    @Test
    void findExpiredSubscriptionsReturnsMappedResults() {
        Instant now = Instant.now();
        Subscription sub = buildSubscription(SubscriptionStatus.ACTIVE, now.minusSeconds(3600));
        when(subscriptionRepository.findBySubscriptionEndBefore(now)).thenReturn(List.of(sub));

        List<SubscriptionExpiry> result = strategy.findExpiredSubscriptions(now);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().subscriptionId()).isEqualTo(sub.getSubscriptionId().toString());
        assertThat(result.getFirst().currentStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findExpiredSubscriptionsReturnsEmptyWhenNoneExpired() {
        when(subscriptionRepository.findBySubscriptionEndBefore(any())).thenReturn(List.of());
        assertThat(strategy.findExpiredSubscriptions(Instant.now())).isEmpty();
    }

    @Test
    void findTerminatedSubscriptionsToPurgeDelegatesToRepository() {
        Instant threshold = Instant.now().minusSeconds(86400);
        Subscription sub = buildSubscription(SubscriptionStatus.TERMINATED, threshold);
        when(subscriptionRepository.findByStatusAndUpdatedAtBefore(SubscriptionStatus.TERMINATED, threshold))
                .thenReturn(List.of(sub));

        List<SubscriptionExpiry> result = strategy.findTerminatedSubscriptionsToPurge(threshold);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().currentStatus()).isEqualTo("TERMINATED");
    }

    @Test
    void terminateSubscriptionDelegatesToService() {
        UUID id = UUID.randomUUID();
        strategy.terminateSubscription(id.toString());
        verify(subscriptionService).terminateSubscription(id);
    }

    @Test
    void purgeSubscriptionDelegatesToService() {
        UUID id = UUID.randomUUID();
        strategy.purgeSubscription(id.toString());
        verify(subscriptionService).purgeSubscription(id);
    }

    private Subscription buildSubscription(SubscriptionStatus status, Instant end) {
        return Subscription.builder()
                .subscriptionId(UUID.randomUUID())
                .status(status)
                .subscriptionEnd(end)
                .build();
    }
}
