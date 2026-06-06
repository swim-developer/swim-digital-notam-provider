package com.github.swim_developer.unit;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.DnotamSubscriptionFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TestNameLoggerExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DnotamSubscriptionFilterTest {

    private static final DnotamEvent RUNWAY_CLOSURE = new DnotamEvent(
            "EVT-001", "RWY.CLS", "EHAM", "EHAA", "A", "EUROCONTROL", "EAD",
            Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z"), "<xml/>");

    @Test
    void nullEventIsRejected() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, null, null);
        assertThat(filter.test(null)).isFalse();
    }

    @Test
    void noFiltersAcceptsAll() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void emptyListsAcceptAll() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(List.of(), List.of(), List.of(), null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void matchingEventScenarioAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(List.of("RWY.CLS"), null, null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void nonMatchingEventScenarioRejects() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(List.of("SAA.ACT"), null, null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isFalse();
    }

    @Test
    void eventScenarioMatchIsCaseInsensitive() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(List.of("rwy.cls"), null, null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void matchingAirportAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, List.of("EHAM"), null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void nonMatchingAirportRejects() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, List.of("LFPG"), null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isFalse();
    }

    @Test
    void matchingAirspaceAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, List.of("EHAA"), null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void nonMatchingAirspaceRejects() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, List.of("LFFF"), null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isFalse();
    }

    @Test
    void matchingEventSeriesAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, "A", null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void nonMatchingEventSeriesRejects() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, "B", null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isFalse();
    }

    @Test
    void matchingPublisherAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, "EUROCONTROL", null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void matchingProviderAccepts() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, null, "EAD");
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void multipleAllowedScenariosOneMatches() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(
                List.of("SAA.ACT", "RWY.CLS", "OBS.NEW"), null, null, null, null, null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void allFiltersMustMatch() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(
                List.of("RWY.CLS"), List.of("EHAM"), List.of("EHAA"), "A", "EUROCONTROL", "EAD");
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void oneFilterMismatchRejects() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(
                List.of("RWY.CLS"), List.of("LFPG"), List.of("EHAA"), "A", "EUROCONTROL", "EAD");
        assertThat(filter.test(RUNWAY_CLOSURE)).isFalse();
    }

    @Test
    void eventWithNullFieldsAndActiveFilterRejects() {
        DnotamEvent sparse = new DnotamEvent("EVT-002", null, null, null, null, null, null, null, null, "<xml/>");
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(List.of("RWY.CLS"), null, null, null, null, null);
        assertThat(filter.test(sparse)).isFalse();
    }

    @Test
    void eventWithNullFieldsAndNoFilterAccepts() {
        DnotamEvent sparse = new DnotamEvent("EVT-002", null, null, null, null, null, null, null, null, "<xml/>");
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, null, null);
        assertThat(filter.test(sparse)).isTrue();
    }

    @Test
    void publisherMatchIsCaseInsensitive() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, "eurocontrol", null);
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void providerMatchIsCaseInsensitive() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, null, null, "ead");
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }

    @Test
    void emptyStringValueActsAsNoFilter() {
        Predicate<DnotamEvent> filter = DnotamSubscriptionFilter.create(null, null, null, "", "", "");
        assertThat(filter.test(RUNWAY_CLOSURE)).isTrue();
    }
}
