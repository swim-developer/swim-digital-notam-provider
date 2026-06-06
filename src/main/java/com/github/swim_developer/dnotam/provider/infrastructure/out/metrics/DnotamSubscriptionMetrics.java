package com.github.swim_developer.dnotam.provider.infrastructure.out.metrics;

import com.github.swim_developer.framework.infrastructure.out.cluster.LeaderElection;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.provider.application.metrics.AbstractSubscriptionMetrics;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class DnotamSubscriptionMetrics extends AbstractSubscriptionMetrics {

    private final MongoSubscriptionStore subscriptionRepository;
    private final LeaderElection leaderElection;

    @Inject
    protected DnotamSubscriptionMetrics(MeterRegistry registry,
                                        MongoSubscriptionStore subscriptionRepository,
                                        LeaderElection leaderElection) {
        super(registry);
        this.subscriptionRepository = subscriptionRepository;
        this.leaderElection = leaderElection;
    }

    protected DnotamSubscriptionMetrics() {
        this(null, null, null);
    }

    private MultiGauge subscriptionsByScenario;
    private MultiGauge subscriptionsByAirport;
    private MultiGauge subscriptionsByAirspace;

    @Override
    protected String getServiceName() {
        return "dnotam";
    }

    @Override
    protected double countActiveSubscriptions() {
        try {
            return subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        } catch (Exception e) {
            log.warn("Failed to count active subscriptions", e);
            return 0;
        }
    }

    @Override
    protected void registerCustomGauges() {
        subscriptionsByScenario = MultiGauge.builder("dnotam_subscriptions_by_scenario")
                .description("Active subscriptions by event scenario filter")
                .register(registry);

        subscriptionsByAirport = MultiGauge.builder("dnotam_subscriptions_by_airport")
                .description("Active subscriptions by airport filter")
                .register(registry);

        subscriptionsByAirspace = MultiGauge.builder("dnotam_subscriptions_by_airspace")
                .description("Active subscriptions by airspace filter")
                .register(registry);
    }

    @Override
    protected void performGaugeUpdate() {
        List<Subscription> active = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        updateScenarioGauges(active);
        updateAirportGauges(active);
        updateAirspaceGauges(active);
    }

    void onStart(@Observes StartupEvent ev) {
        updateGauges();
    }

    @Scheduled(every = "30s")
    void scheduledUpdate() {
        if (!leaderElection.isLeader()) {
            return;
        }
        updateGauges();
    }

    private void updateScenarioGauges(List<Subscription> subscriptions) {
        Map<String, Long> counts = subscriptions.stream()
                .filter(s -> s.getEventScenario() != null && !s.getEventScenario().isEmpty())
                .flatMap(s -> s.getEventScenario().stream())
                .collect(Collectors.groupingBy(scenario -> scenario, Collectors.counting()));

        subscriptionsByScenario.register(counts.entrySet().stream()
                .map(e -> MultiGauge.Row.of(Tags.of("scenario", e.getKey()), e.getValue()))
                .toList(), true);
    }

    private void updateAirportGauges(List<Subscription> subscriptions) {
        Map<String, Long> counts = subscriptions.stream()
                .filter(s -> s.getAirportHeliport() != null && !s.getAirportHeliport().isEmpty())
                .flatMap(s -> s.getAirportHeliport().stream())
                .collect(Collectors.groupingBy(airport -> airport, Collectors.counting()));

        subscriptionsByAirport.register(counts.entrySet().stream()
                .map(e -> MultiGauge.Row.of(Tags.of("airport", e.getKey()), e.getValue()))
                .toList(), true);
    }

    private void updateAirspaceGauges(List<Subscription> subscriptions) {
        Map<String, Long> counts = subscriptions.stream()
                .filter(s -> s.getAirspace() != null && !s.getAirspace().isEmpty())
                .flatMap(s -> s.getAirspace().stream())
                .collect(Collectors.groupingBy(airspace -> airspace, Collectors.counting()));

        subscriptionsByAirspace.register(counts.entrySet().stream()
                .map(e -> MultiGauge.Row.of(Tags.of("airspace", e.getKey()), e.getValue()))
                .toList(), true);
    }
}
