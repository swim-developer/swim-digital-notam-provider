package com.github.swim_developer.dnotam.provider.infrastructure.out.xml;

import aero.aixm.AbstractAIXMFeatureType;
import aero.aixm.AirspacePropertyType;
import aero.aixm.message.AIXMBasicMessageType;
import aero.aixm.message.BasicMessageMemberAIXMPropertyType;
import aero.aixm.event.AISMessagePropertyType;
import aero.aixm.event.AbstractAISMessageType;
import aero.aixm.event.EventTimeSlicePropertyType;
import aero.aixm.event.EventTimeSliceType;
import aero.aixm.event.EventType;
import aero.aixm.event.NOTAMType;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamEvent;
import com.github.swim_developer.framework.application.port.out.SwimEventExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.opengis.gml.TimePeriodType;
import net.opengis.gml.TimePrimitivePropertyType;
import net.opengis.gml.AbstractTimePrimitiveType;
import net.opengis.gml.TimePositionType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class DnotamEventExtractor implements SwimEventExtractor<DnotamEvent, AIXMBasicMessageType> {

    @Override
    public List<Optional<DnotamEvent>> extract(AIXMBasicMessageType message) {
        if (message == null) {
            return List.of(Optional.empty());
        }

        EventType eventType = findEvent(message);
        if (eventType == null) {
            log.warn("No Event element found in AIXM message");
            return List.of(Optional.empty());
        }

        try {
            DnotamEvent event = extractFromEvent(eventType);
            return List.of(Optional.of(event));
        } catch (RuntimeException e) {
            log.error("Failed to extract DNOTAM event from AIXM message", e);
            return List.of(Optional.empty());
        }
    }

    private EventType findEvent(AIXMBasicMessageType message) {
        for (BasicMessageMemberAIXMPropertyType member : message.getHasMember()) {
            JAXBElement<? extends AbstractAIXMFeatureType> feature = member.getAbstractAIXMFeature();
            if (feature != null && feature.getValue() instanceof EventType evt) {
                return evt;
            }
        }
        return null;
    }

    private DnotamEvent extractFromEvent(EventType eventType) {
        String eventId = eventType.getId();

        EventTimeSliceType timeSlice = findFirstTimeSlice(eventType);
        if (timeSlice == null) {
            return new DnotamEvent(eventId, null, null, null, null, null, null, null, null, null);
        }

        String scenario = extractJaxbString(timeSlice.getScenario());
        String airportHeliport = null;
        String airspace = extractAirspaceFromConcernedAirspace(timeSlice);
        String eventSeries = null;

        NOTAMType notam = findFirstNotam(timeSlice);
        if (notam != null) {
            airportHeliport = extractJaxbString(notam.getLocation());
            eventSeries = extractJaxbString(notam.getSeries());
            if (airspace == null) {
                airspace = extractJaxbString(notam.getAffectedFIR());
            }
        }

        Instant validFrom = null;
        Instant validTo = null;
        TimePeriodType period = extractTimePeriod(timeSlice);
        if (period != null) {
            validFrom = parseTimePosition(period.getBeginPosition());
            validTo = parseTimePosition(period.getEndPosition());
        }

        return new DnotamEvent(
                eventId, scenario, airportHeliport, airspace, eventSeries,
                null, null, validFrom, validTo, null
        );
    }

    private EventTimeSliceType findFirstTimeSlice(EventType eventType) {
        List<EventTimeSlicePropertyType> slices = eventType.getTimeSlice();
        if (slices == null || slices.isEmpty()) {
            return null;
        }
        return slices.getFirst().getEventTimeSlice();
    }

    private NOTAMType findFirstNotam(EventTimeSliceType timeSlice) {
        List<AISMessagePropertyType> notifications = timeSlice.getNotification();
        if (notifications == null || notifications.isEmpty()) {
            return null;
        }
        for (AISMessagePropertyType notif : notifications) {
            JAXBElement<? extends AbstractAISMessageType> msg = notif.getAbstractAISMessage();
            if (msg != null && msg.getValue() instanceof NOTAMType notam) {
                return notam;
            }
        }
        return null;
    }

    private String extractAirspaceFromConcernedAirspace(EventTimeSliceType timeSlice) {
        List<AirspacePropertyType> concerned = timeSlice.getConcernedAirspace();
        if (concerned == null || concerned.isEmpty()) {
            return null;
        }
        String href = concerned.getFirst().getHref();
        if (href != null) {
            return extractIcaoFromHref(href);
        }
        return null;
    }

    private TimePeriodType extractTimePeriod(EventTimeSliceType timeSlice) {
        TimePrimitivePropertyType validTime = timeSlice.getValidTime();
        if (validTime == null) {
            return null;
        }
        JAXBElement<? extends AbstractTimePrimitiveType> primitive = validTime.getAbstractTimePrimitive();
        if (primitive != null && primitive.getValue() instanceof TimePeriodType period) {
            return period;
        }
        return null;
    }

    private Instant parseTimePosition(TimePositionType position) {
        if (position == null) {
            return null;
        }
        List<String> values = position.getValue();
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(values.getFirst());
        } catch (Exception e) {
            log.warn("Failed to parse time position: {}", values.getFirst());
            return null;
        }
    }

    private String extractIcaoFromHref(String href) {
        String[] parts = href.split("/");
        String lastPart = parts[parts.length - 1];
        if (lastPart.matches("^[A-Z]{4}$")) {
            return lastPart;
        }
        return null;
    }

    private static String extractJaxbString(JAXBElement<?> element) {
        if (element == null || element.getValue() == null) {
            return null;
        }
        Object val = element.getValue();
        return switch (val) {
            case aero.aixm.TextDesignatorType t -> t.getValue();
            case aero.aixm.CodeUpperAlphaType t -> t.getValue();
            case aero.aixm.CodeICAOType t -> t.getValue();
            case aero.aixm.TextNameType t -> t.getValue();
            case aero.aixm.event.CodeNOTAMType t -> t.getValue();
            default -> val.toString();
        };
    }
}
