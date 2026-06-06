package com.github.swim_developer.dnotam.provider.infrastructure.in.rest;

import java.time.Instant;

class ResolvedParams {
    String scenario;
    String airport;
    String airspace;
    String provider;
    Instant startTime;
    Instant endTime;

    ResolvedParams(String scenario, String airport, String airspace, String provider,
                   Instant startTime, Instant endTime) {
        this.scenario = scenario;
        this.airport = airport;
        this.airspace = airspace;
        this.provider = provider;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
