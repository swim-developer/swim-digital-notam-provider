package com.github.swim_developer.dnotam.provider.infrastructure.out.config;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OpenApiFilter(stages = OpenApiFilter.RunStage.BUILD)
@Slf4j
public class DnotamOpenApiFilter implements OASFilter {

    private static final String DESCRIPTION_KEY = "description";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        openAPI.addExtension("x-service-identification", Map.of(
                "edition", "01.00.00",
                "referenceDate", "2023-06-20",
                "serviceType", "SWIM_DEFINITION",
                "businessActivityType", "INFORMATION_MANAGEMENT",
                "informationCategory", "AERONAUTICAL_INFORMATION_EXCHANGE"
        ));

        Map<String, String> glossary = getGlossary();
        openAPI.addExtension("x-glossary", glossary);

        openAPI.addExtension("x-filtering-capabilities", Map.of(
                "subscription-interface", Map.of(
                        DESCRIPTION_KEY, "The Subscription Interface allows selection of event scenarios of interest organized around specific aerodromes or airspaces.",
                        "filters", List.of("eventScenario", "eventSeries", "provider", "publisher", "airportHeliport", "airspace")
                ),
                "request-interface", Map.of(
                        DESCRIPTION_KEY, "The Request Interface supports filtering based on OGC Filter Encoding 2.0 Encoding Standard. The temporal extension (WFS-TE) is used for advanced temporal filtering.",
                        "standards", List.of("OGC Filter Encoding 2.0", "WFS Temporality Extension (WFS-TE)"),
                        "minimum-use-case", "Retrieve the BASELINE of a feature valid at a point in time"
                )
        ));

        openAPI.addExtension("x-data-quality", Map.of(
                "regulatory-compliance", List.of("Commission Implementing Regulation (EU) 2017/373 of 1 March 2017"),
                "encoding", List.of("Digital NOTAM Specification event scenarios"),
                "guidelines", List.of("AIXM 5.1.1 Common Coding Guidelines", "AIXM 5.1.1 Technical Coding Guidelines"),
                "schema", "https://www.aixm.aero/schema/5.1/message/AIXM_BasicMessage.xsd"
        ));

        openAPI.addExtension("x-message-exchange-patterns", Map.of(
                "publish-subscribe", Map.of(
                        DESCRIPTION_KEY, "A request-reply implementation handles the subscription (Subscription Interface). An AMQP 1.0 implementation handles message distribution (Distribution Interface).",
                        "protocol", "AMQP 1.0",
                        "binding", "SWIM-TI Yellow Profile AMQP Messaging"
                ),
                "synchronous-request-reply", Map.of(
                        DESCRIPTION_KEY, "Request message sent from service consumer to service. Consumer and service remain blocked while processing. AIXM Basic Message returned as reply.",
                        "protocol", "HTTP/REST",
                        "binding", "SWIM-TI Yellow Profile WS-Light",
                        "standard", "OGC Web Feature Service 2.0"
                )
        ));

        openAPI.addExtension("x-service-behaviour", Map.of(
                "subscription-lifecycle", List.of(
                        "Create subscription (status: PAUSED by default)",
                        "Activate subscription (status: ACTIVE)",
                        "Pause subscription (status: PAUSED)",
                        "Resume subscription (status: ACTIVE)",
                        "Delete subscription (permanent removal)"
                ),
                "distribution-flow", List.of(
                        "Event scenario triggered",
                        "System generates Digital NOTAM message (AIXM 5.1.1)",
                        "Message distributed to AMQP queues based on subscription filters",
                        "Consumer connects to queue and consumes message",
                        "Consumer sends acknowledgment, message removed from queue"
                ),
                "request-flow", List.of(
                        "Consumer sends WFS GetFeature query with filters",
                        "Service processes query",
                        "Service returns AIXM Basic Message with matching event features"
                )
        ));

        openAPI.addExtension("x-scope", Map.of(
                "geographic", "EU Member States and Comprehensive Agreement States",
                "airports", "18 airports listed in EU Implementing Regulation 2021/116 (Common Project One)",
                "event-scenarios", List.of(
                        "Runway Closure",
                        "Taxiway Closure",
                        "Stand Status",
                        "Runway Contamination",
                        "Obstacle New",
                        "Navaid Unserviceable",
                        "Airspace Activation"
                )
        ));

        openAPI.setExternalDocs(OASFactory.createExternalDocumentation()
                .url("https://swim-eurocontrol.atlassian.net/wiki/spaces/ASW/pages/60031402/Digital+NOTAM+Subscription+and+Request+Service+-+Service+Definition")
                .description("Official Service Definition (EUROCONTROL)"));
    }

    private static Map<String, String> getGlossary() {
        Map<String, String> glossary = new HashMap<>();
        glossary.put("AISP", "Aeronautical Information Service Provider");
        glossary.put("AIXM", "Aeronautical Information Exchange Model");
        glossary.put("AMQP", "Advanced Message Queuing Protocol");
        glossary.put("ANSP", "Air Navigation Service Provider");
        glossary.put("ARES", "Airspace Reservation");
        glossary.put("ATM", "Air Traffic Management");
        glossary.put("DNOTAM", "Digital Notice to Airmen");
        glossary.put("EACP", "European Aviation Common PKI");
        glossary.put("ePIB", "Electronic Pre-flight Information Bulletin");
        glossary.put("NOTAM", "Notice to Airmen");
        glossary.put("OGC", "Open Geospatial Consortium");
        glossary.put("PKI", "Public Key Infrastructure");
        glossary.put("SASL", "Simple Authentication and Security Layer");
        glossary.put("SESAR", "Single European Sky ATM Research");
        glossary.put("SWIM", "System Wide Information Management");
        glossary.put("TI", "Technical Infrastructure");
        glossary.put("TLS", "Transport Layer Security");
        glossary.put("WFS", "Web Feature Service");
        glossary.put("WFS-TE", "Web Feature Service - Temporality Extension");
        return glossary;
    }
}

