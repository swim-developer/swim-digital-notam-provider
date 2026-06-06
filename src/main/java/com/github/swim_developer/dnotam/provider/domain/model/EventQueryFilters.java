package com.github.swim_developer.dnotam.provider.domain.model;

import java.time.Instant;

public record EventQueryFilters(
        String eventScenario,
        String airportHeliport,
        String airspace,
        String provider,
        Instant startTime,
        Instant endTime,
        int startIndex,
        int count
) {
}
