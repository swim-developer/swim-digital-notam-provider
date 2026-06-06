package com.github.swim_developer.unit;

import aero.aixm.message.AIXMBasicMessageType;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.dnotam.provider.infrastructure.in.amqp.DnotamIngressMessageHandler;
import com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.DnotamEventStore;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamEventExtractor;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamJaxbUnmarshallerPool;
import com.github.swim_developer.framework.domain.exception.XmlValidationException;
import com.github.swim_developer.framework.infrastructure.out.cache.HandoffCache;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Vertx;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TestNameLoggerExtension.class})
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DnotamEventProcessorTest {

    private DnotamIngressMessageHandler processor;

    @Mock
    private DnotamEventStore eventRepository;

    @Mock
    private DnotamJaxbUnmarshallerPool jaxbPool;

    @Mock
    private DnotamEventExtractor eventExtractor;

    @Mock
    private HandoffCache handoffCache;

    @Mock
    private Vertx vertx;

    @Mock
    private TransactionSynchronizationRegistry txSyncRegistry;

    @Spy
    private MeterRegistry registry = new SimpleMeterRegistry();

    private final AIXMBasicMessageType stubParsed = new AIXMBasicMessageType();

    @BeforeEach
    void setUp() throws XmlValidationException {
        processor = new DnotamIngressMessageHandler(
                eventRepository, eventExtractor, jaxbPool,
                handoffCache, vertx,
                registry, txSyncRegistry);

        lenient().when(jaxbPool.unmarshalAndValidate(anyString())).thenReturn(stubParsed);
    }

    @Test
    void processEventWithJaxbValidationFailureIncrementsCounter() throws XmlValidationException {
        when(jaxbPool.unmarshalAndValidate(anyString()))
                .thenThrow(new XmlValidationException("Invalid AIXM"));

        processor.processEvent("<bad/>");

        assertThat(registry.counter("dnotam_events_failed_total", "reason", "jaxb_validation_failed").count())
                .isEqualTo(1.0);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void processEventWithEmptyExtractionIncrementsFailedCounter() {
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.empty()));

        processor.processEvent("<aixm/>");

        assertThat(registry.counter("dnotam_events_failed_total", "reason", "extraction_failed").count())
                .isEqualTo(1.0);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void processEventPersistsNewEntity() {
        DnotamEvent event = new DnotamEvent("EVT-001", "RWY.CLS", "EHAM", null,
                "A", null, null, Instant.now(), Instant.now().plusSeconds(3600), null);
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.of(event)));
        when(eventRepository.findDomainById("EVT-001")).thenReturn(null);

        processor.processEvent("<aixm/>");

        assertThat(registry.counter("dnotam_events_persisted_total").count()).isEqualTo(1.0);
        assertThat(registry.counter("dnotam_events_received_total", "scenario", "closure").count())
                .isEqualTo(1.0);
    }

    @Test
    void processEventUpdatesExistingEntity() {
        DnotamEvent event = new DnotamEvent("EVT-001", "RWY.CLS", "EHAM", null,
                "A", null, null, Instant.now(), Instant.now().plusSeconds(3600), null);
        DnotamStoredEvent existing = DnotamStoredEvent.builder().eventId("EVT-001").build();
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.of(event)));
        when(eventRepository.findDomainById("EVT-001")).thenReturn(existing);

        processor.processEvent("<aixm/>");

        verify(eventRepository, never()).persist((DnotamStoredEvent) any());
        assertThat(existing.getEventScenario()).isEqualTo("RWY.CLS");
        assertThat(existing.getAirportHeliport()).isEqualTo("EHAM");
    }

    @Test
    void processEventDispatchesAfterPersist() {
        DnotamEvent event = new DnotamEvent("EVT-001", "SAA.ACT", null, "EHAA",
                null, null, null, null, null, null);
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.of(event)));
        when(eventRepository.findDomainById("EVT-001")).thenReturn(null);

        processor.processEvent("<aixm/>");

        verify(txSyncRegistry).registerInterposedSynchronization(any());
    }

    @Test
    void processEventHandlesPersistenceFailure() {
        DnotamEvent event = new DnotamEvent("EVT-001", "RWY.CLS", "EHAM", null,
                null, null, null, null, null, null);
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.of(event)));
        when(eventRepository.findDomainById("EVT-001")).thenThrow(new RuntimeException("DB error"));

        processor.processEvent("<aixm/>");

        assertThat(registry.counter("dnotam_events_failed_total", "reason", "persistence_failed").count())
                .isEqualTo(1.0);
        verifyNoInteractions(txSyncRegistry);
    }

    @Test
    void normalizeScenarioCategorizesClosureScenarios() {
        assertScenarioCategory("RWY.CLS", "closure");
        assertScenarioCategory("RWY.CLS.TEMP", "closure");
        assertScenarioCategory("CLOSURE", "closure");
    }

    @Test
    void normalizeScenarioCategorizesRestrictionScenarios() {
        assertScenarioCategory("RWY.LIM", "restriction");
        assertScenarioCategory("RWY.LIM.WEIGHT", "restriction");
        assertScenarioCategory("RESTRICTION", "restriction");
    }

    @Test
    void normalizeScenarioCategorizesAirspaceScenarios() {
        assertScenarioCategory("SAA.ACT", "airspace");
        assertScenarioCategory("AIRSPACE", "airspace");
    }

    @Test
    void normalizeScenarioCategorizesHazardsAndNavaids() {
        assertScenarioCategory("NAV.UNS", "hazards_navaids");
        assertScenarioCategory("OBS.NEW", "hazards_navaids");
        assertScenarioCategory("HAZARD", "hazards_navaids");
    }

    @Test
    void normalizeScenarioCategorizesUnknownAsOthers() {
        assertScenarioCategory("TWY.CLS", "others");
        assertScenarioCategory("SOMETHING_ELSE", "others");
    }

    @Test
    void normalizeScenarioHandlesNullAndBlank() {
        assertScenarioCategory(null, "unknown");
        assertScenarioCategory("", "unknown");
        assertScenarioCategory("   ", "unknown");
    }

    private void assertScenarioCategory(String scenario, String expectedCategory) {
        String registryKey = "dnotam_events_received_total";
        double before = registry.counter(registryKey, "scenario", expectedCategory).count();

        DnotamEvent event = new DnotamEvent("EVT-TEST", scenario, null, null,
                null, null, null, null, null, null);
        when(eventExtractor.extract(any())).thenReturn(List.of(Optional.of(event)));
        when(eventRepository.findDomainById("EVT-TEST")).thenReturn(null);

        processor.processEvent("<aixm/>");

        double after = registry.counter(registryKey, "scenario", expectedCategory).count();
        assertThat(after).isGreaterThan(before);
    }
}
