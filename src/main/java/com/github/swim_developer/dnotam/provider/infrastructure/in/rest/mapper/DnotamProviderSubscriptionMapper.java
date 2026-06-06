package com.github.swim_developer.dnotam.provider.infrastructure.in.rest.mapper;

import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionResult;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionRequest;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DnotamProviderSubscriptionMapper {

    public SubscriptionCommand toCommand(SubscriptionRequest request) {
        return new SubscriptionCommand(
                request.topic(),
                request.qos(),
                request.durable(),
                request.queueName(),
                request.eventScenario(),
                request.airportHeliport(),
                request.airspace(),
                request.eventSeries(),
                request.publisher(),
                request.provider(),
                request.description(),
                request.comment()
        );
    }

    public SubscriptionResponse toResponse(SubscriptionResult result) {
        return new SubscriptionResponse(
                result.topic(),
                result.subscriptionId(),
                result.queue(),
                result.subscriptionStatus(),
                result.qos(),
                result.durable(),
                result.subscriptionEnd(),
                result.providerName(),
                result.heartbeatQueue(),
                result.eventScenario(),
                result.airportHeliport(),
                result.airspace(),
                result.eventSeries(),
                result.publisher(),
                result.description(),
                result.comment()
        );
    }
}
