package com.github.swim_developer.dnotam.provider.application.port.out;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.domain.model.EventQueryFilters;
import com.github.swim_developer.framework.domain.model.EventStatus;

import java.util.List;
import java.util.Optional;

public interface EventStore {

    void persist(DnotamStoredEvent entity);

    void update(DnotamStoredEvent entity);

    DnotamStoredEvent findDomainById(String id);

    Optional<DnotamStoredEvent> findByEventId(String eventId);

    List<DnotamStoredEvent> findByScenario(String eventScenario);

    List<DnotamStoredEvent> findByAirport(String airportHeliport);

    List<DnotamStoredEvent> findByAirspace(String airspace);

    List<DnotamStoredEvent> findByProvider(String provider);

    List<DnotamStoredEvent> findWithFilters(EventQueryFilters filters);

    List<DnotamStoredEvent> findActiveEvents();

    long countByScenario(String eventScenario);

    boolean existsByEventId(String eventId);

    List<DnotamStoredEvent> findByStatus(EventStatus status);

    List<DnotamStoredEvent> findByStatus(EventStatus status, int limit);

    List<DnotamStoredEvent> findPendingDelivery(int limit);

    long countByStatus(EventStatus status);
}
