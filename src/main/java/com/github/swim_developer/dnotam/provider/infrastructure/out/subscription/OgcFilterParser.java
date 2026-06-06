package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.github.swim_developer.framework.infrastructure.out.xml.SafeXmlFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class OgcFilterParser {

    private static final String FES_NS = "http://www.opengis.net/fes/2.0";
    private static final String LOWER_BOUNDARY = "LowerBoundary";
    private static final String UPPER_BOUNDARY = "UpperBoundary";
    private static final String VALUE_REFERENCE = "ValueReference";
    private static final String LITERAL = "Literal";

    public Optional<ParsedFilter> parse(String ogcFilterXml) {
        if (ogcFilterXml == null || ogcFilterXml.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentBuilder builder = SafeXmlFactory.documentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(ogcFilterXml)));

            String eventScenario = extractPropertyEquals(doc, "eventScenario");
            String airportHeliport = extractPropertyEquals(doc, "airportHeliport");
            String airspace = extractPropertyEquals(doc, "airspace");
            String provider = extractPropertyEquals(doc, "provider");
            Instant startTime = extractTemporalBound(doc, "validFrom", LOWER_BOUNDARY);
            Instant endTime = extractTemporalBound(doc, "validTo", UPPER_BOUNDARY);

            if (startTime == null) {
                startTime = extractTemporalBound(doc, "startTime", LOWER_BOUNDARY);
            }
            if (endTime == null) {
                endTime = extractTemporalBound(doc, "endTime", UPPER_BOUNDARY);
            }

            return Optional.of(new ParsedFilter(
                    eventScenario, airportHeliport, airspace, provider, startTime, endTime));

        } catch (Exception e) {
            log.error("Failed to parse OGC Filter XML", e);
            throw new IllegalArgumentException("Invalid OGC Filter XML", e);
        }
    }

    private String extractPropertyEquals(Document doc, String propertyName) {
        NodeList propertyIsEqualTo = doc.getElementsByTagNameNS(FES_NS, "PropertyIsEqualTo");

        for (int i = 0; i < propertyIsEqualTo.getLength(); i++) {
            Element element = (Element) propertyIsEqualTo.item(i);
            String valueRef = getTextContent(element, VALUE_REFERENCE);

            if (propertyName.equals(valueRef)) {
                return getTextContent(element, LITERAL);
            }
        }

        return null;
    }

    private Instant extractTemporalBound(Document doc, String propertyName, String boundaryType) {
        NodeList propertyIsBetween = doc.getElementsByTagNameNS(FES_NS, "PropertyIsBetween");

        for (int i = 0; i < propertyIsBetween.getLength(); i++) {
            Element element = (Element) propertyIsBetween.item(i);
            String valueRef = getTextContent(element, VALUE_REFERENCE);

            if (propertyName.equals(valueRef)) {
                NodeList boundaries = element.getElementsByTagNameNS(FES_NS, boundaryType);
                if (boundaries.getLength() > 0) {
                    Element boundary = (Element) boundaries.item(0);
                    String literal = getTextContent(boundary, LITERAL);
                    return parseInstant(literal);
                }
            }
        }

        NodeList propertyIsGreaterOrEqual = doc.getElementsByTagNameNS(FES_NS, "PropertyIsGreaterThanOrEqualTo");
        for (int i = 0; i < propertyIsGreaterOrEqual.getLength(); i++) {
            Element element = (Element) propertyIsGreaterOrEqual.item(i);
            String valueRef = getTextContent(element, VALUE_REFERENCE);
            if (propertyName.equals(valueRef) && LOWER_BOUNDARY.equals(boundaryType)) {
                return parseInstant(getTextContent(element, LITERAL));
            }
        }

        NodeList propertyIsLessOrEqual = doc.getElementsByTagNameNS(FES_NS, "PropertyIsLessThanOrEqualTo");
        for (int i = 0; i < propertyIsLessOrEqual.getLength(); i++) {
            Element element = (Element) propertyIsLessOrEqual.item(i);
            String valueRef = getTextContent(element, VALUE_REFERENCE);
            if (propertyName.equals(valueRef) && UPPER_BOUNDARY.equals(boundaryType)) {
                return parseInstant(getTextContent(element, LITERAL));
            }
        }

        return null;
    }

    private String getTextContent(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(FES_NS, localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }

        nodes = parent.getElementsByTagName(localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }

        return null;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse temporal value: {}", value);
            return null;
        }
    }
}

