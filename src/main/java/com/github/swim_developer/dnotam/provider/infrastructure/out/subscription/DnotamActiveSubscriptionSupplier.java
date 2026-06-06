package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import com.github.swim_developer.framework.domain.model.ActiveSubscriptionInfo;
import com.github.swim_developer.framework.application.port.out.ActiveSubscriptionSupplier;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class DnotamActiveSubscriptionSupplier implements ActiveSubscriptionSupplier {

    private final MongoSubscriptionStore subscriptionRepository;

    @Inject
    public DnotamActiveSubscriptionSupplier(MongoSubscriptionStore subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<ActiveSubscriptionInfo> getActiveSubscriptions() {
        return subscriptionRepository.findActiveSubscriptions().stream()
                .map(this::toInfo)
                .toList();
    }

    private ActiveSubscriptionInfo toInfo(Subscription sub) {
        return new ActiveSubscriptionInfo(
                sub.getSubscriptionId(),
                sub.getQueue(),
                sub.getStatus().name()
        );
    }
}
