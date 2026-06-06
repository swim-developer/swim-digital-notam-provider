package com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.swim_developer.framework.domain.model.QualityOfService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@RegisterForReflection
public record SubscriptionRequest(
        @NotNull
        @Size(max = 100, message = "Topic must not exceed 100 characters")
        String topic,

        QualityOfService qos,

        Boolean durable,

        @JsonProperty("queue_name")
        @JsonAlias("queueName")
        @Size(max = 255, message = "Queue name must not exceed 255 characters")
        @Pattern(regexp = "^(DNOTAM-[\\w.\\-]+)?$", message = "Queue name must start with 'DNOTAM-' if provided")
        String queueName,

        @JsonProperty("event_scenario")
        @JsonAlias("eventScenario")
        List<String> eventScenario,

        @JsonProperty("airport_heliport")
        @JsonAlias("airportHeliport")
        List<String> airportHeliport,

        @JsonAlias("airSpace")
        List<String> airspace,

        @JsonProperty("event_series")
        @JsonAlias("eventSeries")
        @Size(max = 5, message = "Event series must not exceed 5 characters")
        String eventSeries,

        @Size(max = 100, message = "Publisher must not exceed 100 characters")
        String publisher,

        @Size(max = 100, message = "Provider must not exceed 100 characters")
        String provider,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 500, message = "Comment must not exceed 500 characters")
        String comment
) {
}
