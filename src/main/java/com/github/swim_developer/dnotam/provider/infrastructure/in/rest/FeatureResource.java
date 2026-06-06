package com.github.swim_developer.dnotam.provider.infrastructure.in.rest;

import com.github.swim_developer.dnotam.provider.domain.model.EventQueryFilters;
import com.github.swim_developer.dnotam.provider.application.port.in.QueryEventPort;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.OgcFilterParser;
import com.github.swim_developer.dnotam.provider.infrastructure.out.subscription.ParsedFilter;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@Path("/swim/v1/features")
@Produces(MediaType.APPLICATION_XML)
@SecurityRequirement(name = "mTLS")
@Tag(name = "Request Interface (WFS)", description = "OGC Web Feature Service 2.0 for direct DNOTAM queries. " +
        "Implements wfs.getFeature operation per SWIM-TI Yellow Profile WS-Light binding.")
@Slf4j
public class FeatureResource {

    private final QueryEventPort queryService;
    private final OgcFilterParser filterParser;

    @Inject
    public FeatureResource(QueryEventPort queryService, OgcFilterParser filterParser) {
        this.queryService = queryService;
        this.filterParser = filterParser;
    }

    @GET
    @Operation(
            operationId = "getFeature",
            summary = "Request Digital NOTAMs (WFS GetFeature)",
            description = """
                    OGC Web Feature Service 2.0 interface for querying Digital NOTAMs.
                    Returns AIXM 5.1.1 Basic Message containing Event features and related AIXM features.

                    **SWIM-TI Yellow Profile WS-Light Binding**

                    **Query Options:**
                    1. **Simple filters**: Use individual query parameters (eventScenario, airportHeliport, etc.)
                    2. **OGC Filter**: Use the `filter` parameter with URL-encoded OGC Filter Encoding 2.0 XML

                    **Response Structure (AIXMBasicMessage):**
                    - Container: Single AIXMBasicMessage as root element
                    - Event Features: message:hasMember elements containing event:Event
                    - Related AIXM Features: message:hasMember elements for referenced objects
                    """
    )
    @APIResponse(
            responseCode = "200",
            description = "AIXM 5.1.1 Basic Message containing Event and related AIXM Features",
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML,
                    schema = @Schema(implementation = String.class)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid filter parameters"
    )
    @APIResponse(
            responseCode = "401",
            description = "Authentication failed"
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error"
    )
    public Response getFeature(
            @QueryParam("typeName")
            @Parameter(
                    description = "Feature type to query",
                    example = "event:Event"
            ) String typeName,

            @QueryParam("filter")
            @Parameter(
                    description = "OGC Filter Encoding 2.0 XML (URL-encoded). Alternative to individual filter parameters."
            ) String filter,

            @QueryParam("validTime")
            @Parameter(
                    description = "Validity time filter (WFS-TE). ISO 8601 format.",
                    example = "2025-02-01T00:00:00Z"
            ) String validTime,

            @QueryParam("eventScenario")
            @Parameter(
                    description = "Filter by DNOTAM event scenario code",
                    example = "AD.CLS"
            ) String eventScenario,

            @QueryParam("airportHeliport")
            @Parameter(
                    description = "Filter by ICAO aerodrome code",
                    example = "EADD"
            ) String airportHeliport,

            @QueryParam("airspace")
            @Parameter(
                    description = "Filter by ICAO FIR/UIR code",
                    example = "EAAD"
            ) String airspace,

            @QueryParam("startTime")
            @Parameter(
                    description = "Filter events starting from this time (ISO 8601)",
                    example = "2025-02-01T00:00:00Z"
            ) String startTime,

            @QueryParam("endTime")
            @Parameter(
                    description = "Filter events ending before this time (ISO 8601)",
                    example = "2025-02-10T23:59:59Z"
            ) String endTime,

            @QueryParam("provider")
            @Parameter(
                    description = "Filter by data provider",
                    example = "EADD_AIS"
            ) String provider,

            @QueryParam("startIndex")
            @Parameter(
                    description = "WFS 2.0 pagination: zero-based index of first result to return",
                    example = "0"
            ) Integer startIndex,

            @QueryParam("count")
            @Parameter(
                    description = "WFS 2.0 pagination: maximum number of features to return",
                    example = "100"
            ) Integer count
    ) {
        log.info("WFS GetFeature request - typeName: {}, filter: {}, validTime: {}", typeName, filter != null, validTime);

        ResolvedParams params = new ResolvedParams(
                eventScenario, airportHeliport, airspace, provider,
                parseInstant(startTime), parseInstant(endTime));

        applyValidTime(params, validTime);

        Response errorResponse = applyOgcFilter(params, filter);
        if (errorResponse != null) {
            return errorResponse;
        }

        log.info("Resolved filters - scenario: {}, airport: {}, airspace: {}, from: {}, to: {}, provider: {}",
                params.scenario, params.airport, params.airspace, params.startTime, params.endTime, params.provider);

        int resolvedStartIndex = resolveStartIndex(startIndex);
        int resolvedCount = resolveCount(count);

        return executeQuery(params, resolvedStartIndex, resolvedCount);
    }

    private void applyValidTime(ResolvedParams params, String validTime) {
        if (validTime == null || validTime.isBlank()) {
            return;
        }
        Instant validTimeInstant = parseInstant(validTime);
        if (validTimeInstant != null) {
            params.startTime = validTimeInstant;
            params.endTime = validTimeInstant;
        }
    }

    private Response applyOgcFilter(ResolvedParams params, String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }

        try {
            String decodedFilter = URLDecoder.decode(filter, StandardCharsets.UTF_8);
            var parsedFilter = filterParser.parse(decodedFilter);

            if (parsedFilter.isPresent()) {
                updateParamsFromFilter(params, parsedFilter.get());
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse OGC filter parameter: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildErrorXml("InvalidParameterValue", "Invalid OGC Filter: " + e.getMessage()))
                    .build();
        }
    }

    private void updateParamsFromFilter(ResolvedParams params, ParsedFilter pf) {
        if (pf.eventScenario() != null) params.scenario = pf.eventScenario();
        if (pf.airportHeliport() != null) params.airport = pf.airportHeliport();
        if (pf.airspace() != null) params.airspace = pf.airspace();
        if (pf.provider() != null) params.provider = pf.provider();
        if (pf.startTime() != null) params.startTime = pf.startTime();
        if (pf.endTime() != null) params.endTime = pf.endTime();

        log.debug("Parsed OGC Filter - scenario: {}, airport: {}, airspace: {}",
                pf.eventScenario(), pf.airportHeliport(), pf.airspace());
    }

    private int resolveStartIndex(Integer startIndex) {
        return startIndex != null && startIndex >= 0 ? startIndex : 0;
    }

    private int resolveCount(Integer count) {
        return count != null && count > 0 ? count : 100;
    }

    private Response executeQuery(ResolvedParams params, int startIndex, int count) {
        try {
            EventQueryFilters filters = new EventQueryFilters(
                    params.scenario, params.airport, params.airspace, params.provider,
                    params.startTime, params.endTime, startIndex, count);
            String result = queryService.queryFeatures(filters);

            return Response.ok(result)
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .build();

        } catch (Exception e) {
            log.error("Error executing WFS GetFeature query", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(buildErrorXml("NoApplicableCode", "Query execution failed: " + e.getMessage()))
                    .build();
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}", value);
            return null;
        }
    }

    private String buildErrorXml(String code, String message) {
        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows/2.0" version="2.0.0">
                    <ows:Exception exceptionCode="%s">
                        <ows:ExceptionText>%s</ows:ExceptionText>
                    </ows:Exception>
                </ows:ExceptionReport>
                """, code, message);
    }
}
