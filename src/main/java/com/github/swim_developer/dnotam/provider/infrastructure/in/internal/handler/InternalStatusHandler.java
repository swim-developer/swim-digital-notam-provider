package com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler;

import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.InternalResponseHelper;
import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.SubscriptionSummaryBuilder;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.DnotamEventStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.MongoSubscriptionStore;
import com.github.swim_developer.framework.domain.model.EventStatus;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.infrastructure.out.cluster.LeaderElection;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class InternalStatusHandler {

    private final Vertx vertx;
    private final MongoSubscriptionStore subscriptionRepository;
    private final DnotamEventStore eventRepository;
    private final LeaderElection leaderElection;
    private final SubscriptionSummaryBuilder summaryBuilder;

    @Inject
    public InternalStatusHandler(Vertx vertx,
                                 MongoSubscriptionStore subscriptionRepository,
                                 DnotamEventStore eventRepository,
                                 LeaderElection leaderElection,
                                 SubscriptionSummaryBuilder summaryBuilder) {
        this.vertx = vertx;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.leaderElection = leaderElection;
        this.summaryBuilder = summaryBuilder;
    }

    public void handleStatus(RoutingContext ctx) {
        io.vertx.core.Vertx core = vertx.getDelegate();
        core.getOrCreateContext().executeBlocking(() -> {
            long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
            long received = eventRepository.countByStatus(EventStatus.RECEIVED);
            long delivered = eventRepository.countByStatus(EventStatus.DELIVERED);
            long partiallyDelivered = eventRepository.countByStatus(EventStatus.PARTIALLY_DELIVERED);
            long deadLetter = eventRepository.countByStatus(EventStatus.DEAD_LETTER);
            long totalEvents = eventRepository.count();

            return new JsonObject()
                    .put("status", "UP")
                    .put("leader", leaderElection.isLeader())
                    .put("hostname", leaderElection.getHostname())
                    .put("subscriptions", new JsonObject()
                            .put("active", activeSubscriptions))
                    .put("events", new JsonObject()
                            .put("total", totalEvents)
                            .put("received", received)
                            .put("delivered", delivered)
                            .put("partiallyDelivered", partiallyDelivered)
                            .put("deadLetter", deadLetter));
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                InternalResponseHelper.sendJson(ctx, 200, ar.result());
            } else {
                InternalResponseHelper.sendJson(ctx, 503, new JsonObject()
                        .put("status", "DOWN")
                        .put("error", ar.cause().getMessage()));
            }
        });
    }

    public void handleSubscriptionsSummary(RoutingContext ctx) {
        io.vertx.core.Vertx core = vertx.getDelegate();
        core.getOrCreateContext().executeBlocking(() -> {
            List<Subscription> active = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
            List<Subscription> paused = subscriptionRepository.findByStatus(SubscriptionStatus.PAUSED);

            return new JsonObject()
                    .put("totalActive", active.size())
                    .put("totalPaused", paused.size())
                    .put("byScenario", summaryBuilder.groupByFlattened(active, Subscription::getEventScenario))
                    .put("byAirport", summaryBuilder.groupByFlattened(active, Subscription::getAirportHeliport))
                    .put("byAirspace", summaryBuilder.groupByFlattened(active, Subscription::getAirspace))
                    .put("subscribers", summaryBuilder.buildSubscriberList(active));
        }, false).onComplete(ar -> {
            if (ar.succeeded()) {
                InternalResponseHelper.sendJson(ctx, 200, ar.result());
            } else {
                InternalResponseHelper.sendError(ctx, 500, ar.cause().getMessage());
            }
        });
    }
}
