package com.github.swim_developer.dnotam.provider.infrastructure.in.rest;

import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionRequest;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.mapper.DnotamProviderSubscriptionMapper;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionResponse;
import com.github.swim_developer.dnotam.provider.application.port.in.ManageSubscriptionPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/swim/v1/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
@Tag(name = "Subscriptions", description = "Subscription lifecycle management")
@Slf4j
public class SubscriptionCollectionResource {

    private final ManageSubscriptionPort subscriptionService;
    private final DnotamProviderSubscriptionMapper mapper;
    private final JsonWebToken jwt;

    @Inject
    public SubscriptionCollectionResource(ManageSubscriptionPort subscriptionService,
                                          DnotamProviderSubscriptionMapper mapper,
                                          JsonWebToken jwt) {
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
        this.jwt = jwt;
    }

    @POST
    @Operation(
            operationId = "subscribe",
            summary = "Create a new subscription",
            description = """
                    Allows a service consumer to subscribe to event scenarios of interest.
                    
                    **Subscription Status**: Subscriptions are created in PAUSED status by default. 
                    You must call PUT to ACTIVE status to receive messages.
                    
                    **Queue Name Behavior**:
                    - If `queue_name` is NOT provided: A new queue is created with pattern `DNOTAM-<userId>-<uuid>`
                    - If `queue_name` IS provided and belongs to an ACTIVE/PAUSED subscription of the SAME user: 
                      The queue is reused, allowing multiple subscriptions to deliver messages to the same queue.
                    - If `queue_name` IS provided but belongs to ANOTHER user: A new queue is generated (security protection).
                    - If `queue_name` IS provided but does not exist and follows valid format: 
                      A new queue with the requested name is created.
                    
                    This allows consolidating multiple subscription filters into a single queue for simplified message consumption.
                    """
    )
    @APIResponse(
            responseCode = "201",
            description = "Subscription created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SubscriptionResponse.class)
            )
    )
    @APIResponse(responseCode = "400", description = "Invalid request parameters")
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    @APIResponse(responseCode = "403", description = "Forbidden - user does not have required AMQ role")
    public Response subscribe(
            @Valid @RequestBody(
                    description = "Subscription request payload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SubscriptionRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "simpleSubscription",
                                            summary = "Basic subscription (new queue generated)",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"event_scenario\": [\"RWY.CLS\"], \"airport_heliport\": [\"EADD\"], \"description\": \"Runway closure events for Donlon Airport\"}"
                                    ),
                                    @ExampleObject(
                                            name = "reuseExistingQueue",
                                            summary = "Add filters to existing queue",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"queue_name\": \"DNOTAM-client01-550e8400-e29b-41d4-a716-446655440000\", \"event_scenario\": [\"SAA.ACT\"], \"airspace\": [\"EAAD\"], \"description\": \"Airspace activations on same queue\"}"
                                    ),
                                    @ExampleObject(
                                            name = "fullSubscription",
                                            summary = "Complete subscription with all fields",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"qos\": \"EXACTLY_ONCE\", \"durable\": true, \"queue_name\": \"DNOTAM-MY_CLIENT_ID-ExistingQueueUUID\", \"event_scenario\": [\"RWY.CLS\", \"SAA.ACT\", \"AD.CLS\"], \"airport_heliport\": [\"EHAM\", \"LFPG\"], \"airspace\": [\"EHAA\", \"LFFF\"], \"publisher\": \"EUROCONTROL\", \"provider\": \"EAD\", \"event_series\": \"A\", \"description\": \"Critical alerts for Amsterdam and Paris\", \"comment\": \"Operational desk A\"}"
                                    )
                            }
                    )
            ) SubscriptionRequest request
    ) {
        log.info("Subscription CREATE request for topic: {}", request.topic());
        var command = mapper.toCommand(request);
        var result = subscriptionService.createSubscription(command);
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(result)).build();
    }

    @GET
    @Operation(
            operationId = "getSubscriptions",
            summary = "Get list of subscriptions",
            description = "Returns the list of subscriptions for the authenticated user."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of subscriptions",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SubscriptionResponse.class, type = SchemaType.ARRAY)
            )
    )
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    public Response getSubscriptions() {
        log.info("Subscription LIST - user: {}", jwt.getSubject());
        List<SubscriptionResponse> subscriptions = subscriptionService.listSubscriptions()
                .stream().map(mapper::toResponse).toList();
        return Response.ok(subscriptions).build();
    }

    @GET
    @Path("/ping")
    @Operation(hidden = true)
    public Response ping() {
        return Response.ok("pong").build();
    }
}
