package com.github.swim_developer.dnotam.provider.domain.model;

import com.github.swim_developer.framework.domain.model.QualityOfService;

import java.util.List;

public record SubscriptionCommand(
        String topic,
        QualityOfService qos,
        Boolean durable,
        String queueName,
        List<String> eventScenario,
        List<String> airportHeliport,
        List<String> airspace,
        String eventSeries,
        String publisher,
        String provider,
        String description,
        String comment
) {
}
