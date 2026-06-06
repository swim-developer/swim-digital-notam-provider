package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;

import java.util.List;
import java.util.function.Predicate;

public final class DnotamSubscriptionFilter {

    private DnotamSubscriptionFilter() {
    }

    public static Predicate<DnotamEvent> fromSubscription(Subscription subscription) {
        return create(
                subscription.getEventScenario(),
                subscription.getAirportHeliport(),
                subscription.getAirspace(),
                subscription.getEventSeries(),
                subscription.getPublisher(),
                subscription.getProvider());
    }

    public static Predicate<DnotamEvent> create(
            List<String> eventScenario,
            List<String> airportHeliport,
            List<String> airspace,
            String eventSeries,
            String publisher,
            String provider) {
        return event -> event != null
                && matchesList(eventScenario, event.eventScenario())
                && matchesList(airportHeliport, event.airportHeliport())
                && matchesList(airspace, event.airspace())
                && matchesValue(eventSeries, event.eventSeries())
                && matchesValue(publisher, event.publisher())
                && matchesValue(provider, event.provider());
    }

    private static boolean matchesList(List<String> allowed, String value) {
        if (allowed == null || allowed.isEmpty()) return true;
        if (value == null) return false;
        return allowed.stream().anyMatch(s -> s.equalsIgnoreCase(value));
    }

    private static boolean matchesValue(String expected, String actual) {
        if (expected == null || expected.isEmpty()) return true;
        if (actual == null) return false;
        return actual.equalsIgnoreCase(expected);
    }
}
