package com.github.swim_developer.dnotam.provider.infrastructure.in.internal;

import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class SubscriptionSummaryBuilder {

    public JsonObject groupByFlattened(List<Subscription> subscriptions,
                                       Function<Subscription, List<String>> extractor) {
        Map<String, Long> counts = subscriptions.stream()
                .map(extractor)
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        JsonObject result = new JsonObject();
        counts.forEach(result::put);
        return result;
    }

    public JsonArray buildSubscriberList(List<Subscription> subscriptions) {
        JsonArray array = new JsonArray();
        for (Subscription sub : subscriptions) {
            array.add(new JsonObject()
                    .put("subscriptionId", sub.getSubscriptionId().toString())
                    .put("userId", sub.getUserId())
                    .put("queue", sub.getQueue())
                    .put("topic", sub.getTopic())
                    .put("scenarios", new JsonArray(sub.getEventScenario()))
                    .put("airports", new JsonArray(sub.getAirportHeliport()))
                    .put("airspaces", new JsonArray(sub.getAirspace()))
                    .put("qos", sub.getQos().name()));
        }
        return array;
    }
}
