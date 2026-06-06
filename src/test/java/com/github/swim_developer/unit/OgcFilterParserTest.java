package com.github.swim_developer.unit;

import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.OgcFilterParser;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.ParsedFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(TestNameLoggerExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class OgcFilterParserTest {

    private OgcFilterParser parser;

    @BeforeEach
    void setUp() {
        parser = new OgcFilterParser();
    }

    @Test
    void nullInputReturnsEmpty() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertThat(parser.parse("")).isEmpty();
    }

    @Test
    void parsesPropertyIsEqualToForEventScenario() {
        String xml = ogcFilter(propertyEquals("eventScenario", "RWY.CLS"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().eventScenario()).isEqualTo("RWY.CLS");
    }

    @Test
    void parsesPropertyIsEqualToForAirportHeliport() {
        String xml = ogcFilter(propertyEquals("airportHeliport", "EHAM"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().airportHeliport()).isEqualTo("EHAM");
    }

    @Test
    void parsesPropertyIsEqualToForAirspace() {
        String xml = ogcFilter(propertyEquals("airspace", "EHAA"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().airspace()).isEqualTo("EHAA");
    }

    @Test
    void parsesPropertyIsEqualToForProvider() {
        String xml = ogcFilter(propertyEquals("provider", "EAD"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().provider()).isEqualTo("EAD");
    }

    @Test
    void parsesTemporalBetweenBoundaries() {
        String xml = ogcFilter(
                temporalBetween("validFrom", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z")
                        + temporalBetween("validTo", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().startTime()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        assertThat(result.get().endTime()).isEqualTo(Instant.parse("2025-12-31T23:59:59Z"));
    }

    @Test
    void parsesAlternateTemporalPropertyNames() {
        String xml = ogcFilter(
                temporalBetween("startTime", "2025-06-01T00:00:00Z", "2025-06-30T23:59:59Z"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().startTime()).isEqualTo(Instant.parse("2025-06-01T00:00:00Z"));
    }

    @Test
    void parsesMultipleFiltersInOneDocument() {
        String xml = ogcFilter(
                propertyEquals("eventScenario", "RWY.CLS")
                        + propertyEquals("airportHeliport", "EHAM")
                        + propertyEquals("provider", "EAD"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        ParsedFilter f = result.get();
        assertThat(f.eventScenario()).isEqualTo("RWY.CLS");
        assertThat(f.airportHeliport()).isEqualTo("EHAM");
        assertThat(f.provider()).isEqualTo("EAD");
    }

    @Test
    void missingPropertiesReturnNull() {
        String xml = ogcFilter(propertyEquals("eventScenario", "RWY.CLS"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        ParsedFilter f = result.get();
        assertThat(f.airportHeliport()).isNull();
        assertThat(f.airspace()).isNull();
        assertThat(f.provider()).isNull();
        assertThat(f.startTime()).isNull();
        assertThat(f.endTime()).isNull();
    }

    @Test
    void parsesGreaterThanOrEqualToAsLowerBound() {
        String xml = ogcFilter(comparisonFilter("PropertyIsGreaterThanOrEqualTo", "validFrom", "2025-03-01T00:00:00Z"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().startTime()).isEqualTo(Instant.parse("2025-03-01T00:00:00Z"));
    }

    @Test
    void parsesLessThanOrEqualToAsUpperBound() {
        String xml = ogcFilter(comparisonFilter("PropertyIsLessThanOrEqualTo", "validTo", "2025-03-31T23:59:59Z"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().endTime()).isEqualTo(Instant.parse("2025-03-31T23:59:59Z"));
    }

    @Test
    void invalidTemporalValueReturnsNullTime() {
        String xml = ogcFilter(
                temporalBetween("validFrom", "not-a-date", "also-not-a-date"));
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        assertThat(result.get().startTime()).isNull();
        assertThat(result.get().endTime()).isNull();
    }

    @Test
    void invalidXmlThrowsIllegalArgument() {
        assertThatThrownBy(() -> parser.parse("<not-valid-xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OGC Filter XML");
    }

    @Test
    void emptyFilterDocumentReturnsAllNulls() {
        String xml = ogcFilter("");
        Optional<ParsedFilter> result = parser.parse(xml);

        assertThat(result).isPresent();
        ParsedFilter f = result.get();
        assertThat(f.eventScenario()).isNull();
        assertThat(f.airportHeliport()).isNull();
        assertThat(f.airspace()).isNull();
        assertThat(f.provider()).isNull();
        assertThat(f.startTime()).isNull();
        assertThat(f.endTime()).isNull();
    }

    private static String ogcFilter(String body) {
        return """
                <fes:Filter xmlns:fes="http://www.opengis.net/fes/2.0">
                %s
                </fes:Filter>
                """.formatted(body);
    }

    private static String propertyEquals(String property, String value) {
        return """
                <fes:PropertyIsEqualTo>
                    <fes:ValueReference>%s</fes:ValueReference>
                    <fes:Literal>%s</fes:Literal>
                </fes:PropertyIsEqualTo>
                """.formatted(property, value);
    }

    private static String temporalBetween(String property, String lower, String upper) {
        return """
                <fes:PropertyIsBetween>
                    <fes:ValueReference>%s</fes:ValueReference>
                    <fes:LowerBoundary><fes:Literal>%s</fes:Literal></fes:LowerBoundary>
                    <fes:UpperBoundary><fes:Literal>%s</fes:Literal></fes:UpperBoundary>
                </fes:PropertyIsBetween>
                """.formatted(property, lower, upper);
    }

    private static String comparisonFilter(String operator, String property, String value) {
        return """
                <fes:%s>
                    <fes:ValueReference>%s</fes:ValueReference>
                    <fes:Literal>%s</fes:Literal>
                </%s>
                """.formatted(operator, property, value, "fes:" + operator);
    }
}
