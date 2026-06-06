package com.github.swim_developer.dnotam.provider.domain.model;

import com.github.swim_developer.framework.domain.model.QualityOfService;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResult(
        String topic,
        UUID subscriptionId,
        String queue,
        SubscriptionStatus subscriptionStatus,
        QualityOfService qos,
        Boolean durable,
        Instant subscriptionEnd,
        String providerName,
        String heartbeatQueue,
        List<String> eventScenario,
        List<String> airportHeliport,
        List<String> airspace,
        String eventSeries,
        String publisher,
        String description,
        String comment
) {
}
