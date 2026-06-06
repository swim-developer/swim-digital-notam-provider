package com.github.swim_developer.unit;

import aero.aixm.message.AIXMBasicMessageType;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamEventExtractor;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.validation.AixmUnmarshallerPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TestNameLoggerExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class DnotamEventExtractorTest {

    private static AixmUnmarshallerPool pool;
    private DnotamEventExtractor extractor;

    @BeforeAll
    static void initPool() {
        pool = new AixmUnmarshallerPool();
    }

    @BeforeEach
    void setUp() {
        extractor = new DnotamEventExtractor();
    }

    private AIXMBasicMessageType unmarshal(String xml) throws AixmUnmarshallerPool.AixmUnmarshalException {
        return pool.unmarshalAndValidate(xml);
    }

    private DnotamEvent extractFirst(String xml) throws AixmUnmarshallerPool.AixmUnmarshalException {
        AIXMBasicMessageType parsed = unmarshal(xml);
        List<Optional<DnotamEvent>> results = extractor.extract(parsed);
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst()).isPresent();
        return results.getFirst().get();
    }

    @Test
    void extractsEventFromFullAixmMessage() throws Exception {
        DnotamEvent event = extractFirst(FULL_AIXM);

        assertThat(event.eventId()).isEqualTo("uuid.a0805c34-5a0d-4d00-b689-89a56e25d4ec");
        assertThat(event.eventScenario()).isEqualTo("AD.CLS");
        assertThat(event.validFrom()).isEqualTo(Instant.parse("2026-01-07T10:35:25Z"));
        assertThat(event.validTo()).isEqualTo(Instant.parse("2026-01-09T22:35:25Z"));
    }

    @Test
    void extractsEventSeriesFromNotamElement() throws Exception {
        DnotamEvent event = extractFirst(FULL_AIXM);

        assertThat(event.eventSeries()).isEqualTo("A");
    }

    @Test
    void extractsAirportCodeFromNotamLocation() throws Exception {
        DnotamEvent event = extractFirst(FULL_AIXM);

        assertThat(event.airportHeliport()).isEqualTo("EHAM");
    }

    @Test
    void extractsAirspaceFromAffectedFir() throws Exception {
        DnotamEvent event = extractFirst(FULL_AIXM);

        assertThat(event.airspace()).isEqualTo("EDGG");
    }

    @Test
    void extractsAirspaceFromConcernedAirspace() throws Exception {
        DnotamEvent event = extractFirst(AIXM_WITH_CONCERNED_AIRSPACE);

        assertThat(event.airspace()).isEqualTo("EHAA");
    }

    @Test
    void returnsEmptyOptionalForNullMessage() {
        List<Optional<DnotamEvent>> results = extractor.extract(null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).isEmpty();
    }

    @Test
    void returnsEmptyOptionalForMessageWithoutEvent() throws Exception {
        AIXMBasicMessageType parsed = unmarshal(AIXM_NO_EVENT);
        List<Optional<DnotamEvent>> results = extractor.extract(parsed);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).isEmpty();
    }

    @Test
    void extractsScenarioFromMinimalEvent() throws Exception {
        DnotamEvent event = extractFirst(MINIMAL_EVENT);

        assertThat(event.eventScenario()).isEqualTo("RWY.CLS");
        assertThat(event.airportHeliport()).isNull();
        assertThat(event.eventSeries()).isNull();
    }

    private static final String FULL_AIXM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1.1/message"
                xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
                xmlns:event="http://www.aixm.aero/schema/5.1.1/event" xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                gml:id="M00001">
                <message:hasMember>
                    <event:Event gml:id="uuid.a0805c34-5a0d-4d00-b689-89a56e25d4ec">
                        <gml:identifier codeSpace="urn:uuid:">a0805c34-5a0d-4d00-b689-89a56e25d4ec</gml:identifier>
                        <event:timeSlice>
                            <event:EventTimeSlice gml:id="ID_GEN_00001_01">
                                <gml:validTime>
                                    <gml:TimePeriod gml:id="ID_GEN_00001_02">
                                        <gml:beginPosition>2026-01-07T10:35:25Z</gml:beginPosition>
                                        <gml:endPosition>2026-01-09T22:35:25Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </gml:validTime>
                                <aixm:interpretation>BASELINE</aixm:interpretation>
                                <aixm:sequenceNumber>1</aixm:sequenceNumber>
                                <aixm:correctionNumber>0</aixm:correctionNumber>
                                <aixm:featureLifetime>
                                    <gml:TimePeriod gml:id="ID_GEN_00001_03">
                                        <gml:beginPosition>2026-01-07T10:35:25Z</gml:beginPosition>
                                        <gml:endPosition>2026-01-09T22:35:25Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </aixm:featureLifetime>
                                <event:scenario>AD.CLS</event:scenario>
                                <event:version>2.0</event:version>
                                <event:concernedAirportHeliport xlink:href="urn:uuid:1b54b2d6-a5ff-4e57-94c2-f4047a381c64" xlink:title="AMSTERDAM/SCHIPHOL"/>
                                <event:notification>
                                    <event:NOTAM gml:id="ID_GEN_00001_04">
                                        <event:series>A</event:series>
                                        <event:number>3871</event:number>
                                        <event:year>2026</event:year>
                                        <event:type>N</event:type>
                                        <event:affectedFIR>EDGG</event:affectedFIR>
                                        <event:location>EHAM</event:location>
                                        <event:text>AD closed due to security incident.</event:text>
                                    </event:NOTAM>
                                </event:notification>
                            </event:EventTimeSlice>
                        </event:timeSlice>
                    </event:Event>
                </message:hasMember>
            </message:AIXMBasicMessage>
            """;

    private static final String AIXM_WITH_CONCERNED_AIRSPACE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1.1/message"
                xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
                xmlns:event="http://www.aixm.aero/schema/5.1.1/event" xmlns:xlink="http://www.w3.org/1999/xlink"
                gml:id="M00002">
                <message:hasMember>
                    <event:Event gml:id="EVT-AIRSPACE-001">
                        <event:timeSlice>
                            <event:EventTimeSlice gml:id="TS_002">
                                <gml:validTime>
                                    <gml:TimePeriod gml:id="TP_002">
                                        <gml:beginPosition>2026-01-10T00:00:00Z</gml:beginPosition>
                                        <gml:endPosition>2026-01-11T00:00:00Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </gml:validTime>
                                <aixm:interpretation>BASELINE</aixm:interpretation>
                                <aixm:sequenceNumber>1</aixm:sequenceNumber>
                                <aixm:correctionNumber>0</aixm:correctionNumber>
                                <aixm:featureLifetime>
                                    <gml:TimePeriod gml:id="FL_002">
                                        <gml:beginPosition>2026-01-10T00:00:00Z</gml:beginPosition>
                                        <gml:endPosition>2026-01-11T00:00:00Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </aixm:featureLifetime>
                                <event:scenario>SAA.ACT</event:scenario>
                                <event:version>2.0</event:version>
                                <event:concernedAirspace xlink:href="urn:uuid:Airspace/EHAA"/>
                            </event:EventTimeSlice>
                        </event:timeSlice>
                    </event:Event>
                </message:hasMember>
            </message:AIXMBasicMessage>
            """;

    private static final String AIXM_NO_EVENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1.1/message"
                xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
                gml:id="M00003">
                <message:hasMember>
                    <aixm:AirportHeliport gml:id="AHP-001">
                        <gml:identifier codeSpace="urn:uuid:">x</gml:identifier>
                        <aixm:timeSlice>
                            <aixm:AirportHeliportTimeSlice gml:id="AHP-TS-001">
                                <gml:validTime>
                                    <gml:TimePeriod gml:id="TP_003">
                                        <gml:beginPosition>2026-01-01T00:00:00Z</gml:beginPosition>
                                        <gml:endPosition>2026-12-31T23:59:59Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </gml:validTime>
                                <aixm:interpretation>BASELINE</aixm:interpretation>
                                <aixm:sequenceNumber>1</aixm:sequenceNumber>
                                <aixm:correctionNumber>0</aixm:correctionNumber>
                            </aixm:AirportHeliportTimeSlice>
                        </aixm:timeSlice>
                    </aixm:AirportHeliport>
                </message:hasMember>
            </message:AIXMBasicMessage>
            """;

    private static final String MINIMAL_EVENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1.1/message"
                xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
                xmlns:event="http://www.aixm.aero/schema/5.1.1/event"
                gml:id="M00004">
                <message:hasMember>
                    <event:Event gml:id="EVT-MINIMAL">
                        <event:timeSlice>
                            <event:EventTimeSlice gml:id="TS_004">
                                <gml:validTime>
                                    <gml:TimePeriod gml:id="TP_004">
                                        <gml:beginPosition>2026-02-01T00:00:00Z</gml:beginPosition>
                                        <gml:endPosition>2026-02-02T00:00:00Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </gml:validTime>
                                <aixm:interpretation>BASELINE</aixm:interpretation>
                                <aixm:sequenceNumber>1</aixm:sequenceNumber>
                                <aixm:correctionNumber>0</aixm:correctionNumber>
                                <aixm:featureLifetime>
                                    <gml:TimePeriod gml:id="FL_004">
                                        <gml:beginPosition>2026-02-01T00:00:00Z</gml:beginPosition>
                                        <gml:endPosition>2026-02-02T00:00:00Z</gml:endPosition>
                                    </gml:TimePeriod>
                                </aixm:featureLifetime>
                                <event:scenario>RWY.CLS</event:scenario>
                                <event:version>2.0</event:version>
                            </event:EventTimeSlice>
                        </event:timeSlice>
                    </event:Event>
                </message:hasMember>
            </message:AIXMBasicMessage>
            """;
}
