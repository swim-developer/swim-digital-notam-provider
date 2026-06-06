package com.github.swim_developer.dnotam.provider.infrastructure.out.persistence;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.domain.model.EventQueryFilters;
import com.github.swim_developer.dnotam.provider.application.port.out.EventStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity.DnotamEventJpaEntity;
import com.github.swim_developer.framework.domain.model.EventStatus;
import com.github.swim_developer.framework.domain.model.SwimProviderEvent;
import com.github.swim_developer.framework.provider.application.port.out.SwimProviderEventStorePort;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DnotamEventStore implements PanacheRepositoryBase<DnotamEventJpaEntity, String>, EventStore, SwimProviderEventStorePort {

    private static final String EVENT_SCENARIO_FIELD = "eventScenario";

    private final DnotamProviderPersistenceMapper mapper;

    @Inject
    public DnotamEventStore(DnotamProviderPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<DnotamStoredEvent> findByEventId(String eventId) {
        return findByIdOptional(eventId).map(mapper::toDomain);
    }

    public List<DnotamStoredEvent> findByScenario(String eventScenario) {
        return list(EVENT_SCENARIO_FIELD, eventScenario).stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findByAirport(String airportHeliport) {
        return list("airportHeliport", airportHeliport).stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findByAirspace(String airspace) {
        return list("airspace", airspace).stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findByProvider(String provider) {
        return list("provider", provider).stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findWithFilters(EventQueryFilters filters) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (filters.eventScenario() != null && !filters.eventScenario().isBlank()) {
            query.append(" AND eventScenario = :eventScenario");
            params.put(EVENT_SCENARIO_FIELD, filters.eventScenario());
        }
        if (filters.airportHeliport() != null && !filters.airportHeliport().isBlank()) {
            query.append(" AND airportHeliport = :airportHeliport");
            params.put("airportHeliport", filters.airportHeliport());
        }
        if (filters.airspace() != null && !filters.airspace().isBlank()) {
            query.append(" AND airspace = :airspace");
            params.put("airspace", filters.airspace());
        }
        if (filters.provider() != null && !filters.provider().isBlank()) {
            query.append(" AND provider = :provider");
            params.put("provider", filters.provider());
        }
        if (filters.startTime() != null) {
            query.append(" AND (validTo IS NULL OR validTo >= :startTime)");
            params.put("startTime", filters.startTime());
        }
        if (filters.endTime() != null) {
            query.append(" AND (validFrom IS NULL OR validFrom <= :endTime)");
            params.put("endTime", filters.endTime());
        }
        query.append(" ORDER BY receivedAt DESC");

        int page = (filters.count() > 0) ? filters.startIndex() / filters.count() : 0;
        return find(query.toString(), params).page(page, filters.count()).list()
                .stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findActiveEvents() {
        Instant now = Instant.now();
        return list("(validFrom IS NULL OR validFrom <= ?1) AND (validTo IS NULL OR validTo >= ?1)", now)
                .stream().map(mapper::toDomain).toList();
    }

    public long countByScenario(String eventScenario) {
        return count(EVENT_SCENARIO_FIELD, eventScenario);
    }

    public boolean existsByEventId(String eventId) {
        return count("eventId", eventId) > 0;
    }

    public List<DnotamStoredEvent> findByStatus(EventStatus status) {
        return list("status", status).stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findByStatus(EventStatus status, int limit) {
        return find("status = ?1 ORDER BY receivedAt ASC", status).page(0, limit).list()
                .stream().map(mapper::toDomain).toList();
    }

    public List<DnotamStoredEvent> findPendingDelivery(int limit) {
        return find("status IN (?1, ?2) ORDER BY receivedAt ASC",
                EventStatus.RECEIVED, EventStatus.PARTIALLY_DELIVERED)
                .page(0, limit).list().stream().map(mapper::toDomain).toList();
    }

    public long countByStatus(EventStatus status) {
        return count("status", status);
    }

    @Override
    public void persist(DnotamStoredEvent domain) {
        DnotamEventJpaEntity jpa = mapper.toJpa(domain);
        persistAndFlush(jpa);
    }

    @Override
    public void update(DnotamStoredEvent domain) {
        DnotamEventJpaEntity jpa = mapper.toJpa(domain);
        getEntityManager().merge(jpa);
    }

    @Override
    public DnotamStoredEvent findDomainById(String id) {
        return findByIdOptional(id).map(mapper::toDomain).orElse(null);
    }

    public DnotamStoredEvent mergeDomainEntity(DnotamStoredEvent domain) {
        DnotamEventJpaEntity jpa = mapper.toJpa(domain);
        DnotamEventJpaEntity merged = getEntityManager().merge(jpa);
        return mapper.toDomain(merged);
    }
}
