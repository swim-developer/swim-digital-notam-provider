package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.domain.model.SubscriptionExpiry;
import com.github.swim_developer.framework.application.port.out.SubscriptionExpiryStrategy;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.github.swim_developer.dnotam.provider.application.usecase.DnotamSubscriptionUseCase;

@Slf4j
@ApplicationScoped
public class DnotamExpiryStrategy implements SubscriptionExpiryStrategy {

    private final MongoSubscriptionStore subscriptionRepository;
    private final DnotamSubscriptionUseCase subscriptionService;

    @Inject
    public DnotamExpiryStrategy(MongoSubscriptionStore subscriptionRepository,
                                DnotamSubscriptionUseCase subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public List<SubscriptionExpiry> findExpiredSubscriptions(Instant now) {
        return subscriptionRepository.findBySubscriptionEndBefore(now)
                .stream()
                .map(this::toSubscriptionExpiry)
                .toList();
    }

    @Override
    public List<SubscriptionExpiry> findTerminatedSubscriptionsToPurge(Instant threshold) {
        return subscriptionRepository.findByStatusAndUpdatedAtBefore(SubscriptionStatus.TERMINATED, threshold)
                .stream()
                .map(this::toSubscriptionExpiry)
                .toList();
    }

    @Override
    public void terminateSubscription(String subscriptionId) {
        subscriptionService.terminateSubscription(UUID.fromString(subscriptionId));
    }

    @Override
    public void purgeSubscription(String subscriptionId) {
        subscriptionService.purgeSubscription(UUID.fromString(subscriptionId));
    }

    // ========== HELPER METHODS ==========

    private SubscriptionExpiry toSubscriptionExpiry(Subscription subscription) {
        return new SubscriptionExpiry(
                subscription.getSubscriptionId().toString(),
                subscription.getSubscriptionEnd(),
                subscription.getStatus().name()
        );
    }
}
