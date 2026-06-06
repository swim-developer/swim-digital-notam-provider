package com.github.swim_developer.dnotam.provider.infrastructure.out.persistence;

import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.application.port.out.SubscriptionStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity.SubscriptionJpaEntity;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MongoSubscriptionStore implements PanacheRepositoryBase<SubscriptionJpaEntity, UUID>, SubscriptionStore {

    private final DnotamProviderPersistenceMapper mapper;

    @Inject
    public MongoSubscriptionStore(DnotamProviderPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<Subscription> findByQueue(String queue) {
        return find("queue", queue).firstResultOptional().map(mapper::toDomain);
    }

    public List<Subscription> findByUserId(String userId) {
        return list("userId", userId).stream().map(mapper::toDomain).toList();
    }

    public List<Subscription> findByStatus(SubscriptionStatus status) {
        return list("status", status).stream().map(mapper::toDomain).toList();
    }

    public List<Subscription> findActiveSubscriptions() {
        return list("status in ?1", List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED))
                .stream().map(mapper::toDomain).toList();
    }

    public boolean existsByQueue(String queue) {
        return count("queue", queue) > 0;
    }

    public boolean existsBySubscriptionHash(String subscriptionHash) {
        return count("subscriptionHash", subscriptionHash) > 0;
    }

    public Optional<Subscription> findBySubscriptionHash(String subscriptionHash) {
        return find("subscriptionHash", subscriptionHash).firstResultOptional().map(mapper::toDomain);
    }

    public long countByStatus(SubscriptionStatus status) {
        return count("status", status);
    }

    public Optional<Subscription> findActiveOrPausedByQueueAndUser(String queue, String userId) {
        return find("queue = ?1 AND userId = ?2 AND status IN (?3, ?4)",
                queue, userId, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
                .firstResultOptional().map(mapper::toDomain);
    }

    public boolean existsActiveOrPausedByQueue(String queue) {
        return count("queue = ?1 AND status IN (?2, ?3)",
                queue, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED) > 0;
    }

    public List<Subscription> findBySubscriptionEndBefore(Instant threshold) {
        return list("subscriptionEnd < ?1 and (status = ?2 or status = ?3)",
                    threshold, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED)
                .stream().map(mapper::toDomain).toList();
    }

    public List<Subscription> findByStatusAndUpdatedAtBefore(SubscriptionStatus status, Instant threshold) {
        return list("status = ?1 and updatedAt < ?2", status, threshold)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void persist(Subscription domain) {
        SubscriptionJpaEntity jpa = mapper.toJpa(domain);
        if (jpa.getSubscriptionId() == null) {
            persistAndFlush(jpa);
            domain.setSubscriptionId(jpa.getSubscriptionId());
        } else {
            getEntityManager().merge(jpa);
        }
    }

    @Override
    public void delete(Subscription domain) {
        findByIdOptional(domain.getSubscriptionId()).ifPresent(this::delete);
    }

    @Override
    public Optional<Subscription> findSubscriptionById(UUID id) {
        return findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findEntityById(UUID id) {
        return findSubscriptionById(id);
    }

    @Override
    public Optional<Subscription> findByHash(String hash) {
        return findBySubscriptionHash(hash);
    }
}
