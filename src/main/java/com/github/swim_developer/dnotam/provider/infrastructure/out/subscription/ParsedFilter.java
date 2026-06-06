package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import java.time.Instant;

public record ParsedFilter(
        String eventScenario,
        String airportHeliport,
        String airspace,
        String provider,
        Instant startTime,
        Instant endTime
) {}
