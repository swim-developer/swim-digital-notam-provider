package com.github.swim_developer.dnotam.provider.domain.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public record DnotamEvent(
        String eventId,
        String eventScenario,
        String airportHeliport,
        String airspace,
        String eventSeries,
        String publisher,
        String provider,
        Instant validFrom,
        Instant validTo,
        String aixmMessage
) {
}
