package com.github.swim_developer.dnotam.provider.application.port.out;

import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionResult;

public interface DnotamSubscriptionMappingPort {

    Subscription toEntity(SubscriptionCommand command, String userId, String subscriptionHash, String resolvedQueueName);

    SubscriptionResult toResponse(Subscription subscription);
}
