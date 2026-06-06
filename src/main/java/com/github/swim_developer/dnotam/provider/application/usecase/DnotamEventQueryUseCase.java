package com.github.swim_developer.dnotam.provider.application.usecase;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.application.port.in.QueryEventPort;
import com.github.swim_developer.dnotam.provider.application.port.out.AixmMessageAssemblerPort;
import com.github.swim_developer.dnotam.provider.application.port.out.EventStore;
import com.github.swim_developer.dnotam.provider.domain.model.EventQueryFilters;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class DnotamEventQueryUseCase implements QueryEventPort {

    private final EventStore eventRepository;
    private final AixmMessageAssemblerPort assembler;
    private final MeterRegistry registry;

    @Inject
    public DnotamEventQueryUseCase(EventStore eventRepository,
                                   AixmMessageAssemblerPort assembler,
                                   MeterRegistry registry) {
        this.eventRepository = eventRepository;
        this.assembler = assembler;
        this.registry = registry;
    }

    public String queryFeatures(EventQueryFilters filters) {
        Timer.Sample timerSample = Timer.start(registry);

        List<DnotamStoredEvent> events = eventRepository.findWithFilters(filters);

        log.info("WFS GetFeature query returned {} events (startIndex={}, count={})",
                events.size(), filters.startIndex(), filters.count());

        String result = assembler.assemble(events);

        timerSample.stop(Timer.builder("dnotam_wfs_query_duration")
                .description("Time to execute WFS GetFeature query")
                .tag("resultCount", String.valueOf(events.size()))
                .register(registry));

        return result;
    }

    public Optional<String> findByEventId(String eventId) {
        return eventRepository.findByEventId(eventId)
                .map(DnotamStoredEvent::getAixmMessage);
    }
}

