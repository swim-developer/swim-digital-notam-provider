package com.github.swim_developer.unit;

import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.DnotamSubscriptionHashCalculator;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TestNameLoggerExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class SubscriptionHashCalculatorTest {

    private DnotamSubscriptionHashCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DnotamSubscriptionHashCalculator();
    }

    @Test
    void sameRequestSameUserProducesSameHash() {
        var request = buildRequest("DigitalNOTAMService", List.of("RWY.CLS"), List.of("EHAM"), null);

        String hash1 = calculator.calculateHash(request, "user1");
        String hash2 = calculator.calculateHash(request, "user1");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentUserProducesDifferentHash() {
        var request = buildRequest("DigitalNOTAMService", List.of("RWY.CLS"), List.of("EHAM"), null);

        String hash1 = calculator.calculateHash(request, "user1");
        String hash2 = calculator.calculateHash(request, "user2");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void differentTopicProducesDifferentHash() {
        var req1 = buildRequest("DigitalNOTAMService", List.of("RWY.CLS"), List.of("EHAM"), null);
        var req2 = buildRequest("OtherService", List.of("RWY.CLS"), List.of("EHAM"), null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isNotEqualTo(calculator.calculateHash(req2, "user1"));
    }

    @Test
    void listOrderIsIrrelevantForHash() {
        var req1 = buildRequest("DigitalNOTAMService", List.of("RWY.CLS", "SAA.ACT"), List.of("EHAM", "LFPG"), null);
        var req2 = buildRequest("DigitalNOTAMService", List.of("SAA.ACT", "RWY.CLS"), List.of("LFPG", "EHAM"), null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isEqualTo(calculator.calculateHash(req2, "user1"));
    }

    @Test
    void nullListsProduceConsistentHash() {
        var req1 = new SubscriptionCommand("DigitalNOTAMService", null, null, null, null, null, null, null, null, null, null, null);
        var req2 = new SubscriptionCommand("DigitalNOTAMService", null, null, null, null, null, null, null, null, null, null, null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isEqualTo(calculator.calculateHash(req2, "user1"));
    }

    @Test
    void emptyListsProduceConsistentHash() {
        var request = buildRequest("DigitalNOTAMService", List.of(), List.of(), null);

        String hash1 = calculator.calculateHash(request, "user1");
        String hash2 = calculator.calculateHash(request, "user1");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashIsSha256HexString() {
        var request = buildRequest("DigitalNOTAMService", List.of("RWY.CLS"), List.of("EHAM"), null);
        String hash = calculator.calculateHash(request, "user1");

        assertThat(hash)
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    void differentEventScenarioProducesDifferentHash() {
        var req1 = buildRequest("DigitalNOTAMService", List.of("RWY.CLS"), List.of("EHAM"), null);
        var req2 = buildRequest("DigitalNOTAMService", List.of("SAA.ACT"), List.of("EHAM"), null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isNotEqualTo(calculator.calculateHash(req2, "user1"));
    }

    @Test
    void eventSeriesAffectsHash() {
        var req1 = new SubscriptionCommand("DigitalNOTAMService", null, null, null,
                List.of("RWY.CLS"), List.of("EHAM"), null, "A", null, null, null, null);
        var req2 = new SubscriptionCommand("DigitalNOTAMService", null, null, null,
                List.of("RWY.CLS"), List.of("EHAM"), null, "B", null, null, null, null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isNotEqualTo(calculator.calculateHash(req2, "user1"));
    }

    @Test
    void publisherAndProviderAffectHash() {
        var req1 = new SubscriptionCommand("DigitalNOTAMService", null, null, null,
                List.of("RWY.CLS"), null, null, null, "EUROCONTROL", "EAD", null, null);
        var req2 = new SubscriptionCommand("DigitalNOTAMService", null, null, null,
                List.of("RWY.CLS"), null, null, null, "OTHER", "EAD", null, null);

        assertThat(calculator.calculateHash(req1, "user1"))
                .isNotEqualTo(calculator.calculateHash(req2, "user1"));
    }

    private SubscriptionCommand buildRequest(String topic, List<String> scenarios,
                                             List<String> airports, List<String> airspace) {
        return new SubscriptionCommand(topic, null, null, null, scenarios, airports, airspace,
                null, null, null, null, null);
    }
}
