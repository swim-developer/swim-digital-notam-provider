package com.github.swim_developer.dnotam.provider.domain.model;

import com.github.swim_developer.framework.domain.model.EventStatus;
import com.github.swim_developer.framework.domain.model.SwimProviderEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnotamStoredEvent implements SwimProviderEvent {

    private String eventId;
    private String eventScenario;
    private String airportHeliport;
    private String airspace;
    private String eventSeries;
    private String publisher;
    private String provider;
    private Instant validFrom;
    private Instant validTo;

    @Builder.Default
    private EventStatus status = EventStatus.RECEIVED;

    @Builder.Default
    private Instant receivedAt = Instant.now();

    private Instant processedAt;

    @Builder.Default
    private int deliveredCount = 0;

    @Builder.Default
    private int retryCount = 0;

    private String aixmMessage;

    @Override
    public String getPayload() {
        return aixmMessage;
    }
}
