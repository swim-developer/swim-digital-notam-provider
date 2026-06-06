package com.github.swim_developer.dnotam.provider.infrastructure.in.rest;

import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.mapper.DnotamProviderSubscriptionMapper;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionResponse;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionStatusUpdate;
import com.github.swim_developer.dnotam.provider.application.port.in.ManageSubscriptionPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/swim/v1/subscriptions/{subscriptionId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
@Tag(name = "Subscriptions", description = "Subscription lifecycle management")
@Slf4j
public class SubscriptionItemResource {

    private final ManageSubscriptionPort subscriptionService;
    private final DnotamProviderSubscriptionMapper mapper;
    private final JsonWebToken jwt;

    @Inject
    public SubscriptionItemResource(ManageSubscriptionPort subscriptionService,
                                    DnotamProviderSubscriptionMapper mapper,
                                    JsonWebToken jwt) {
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
        this.jwt = jwt;
    }

    @GET
    @Operation(
            operationId = "getSubscription",
            summary = "Get subscription details",
            description = "Returns detailed information about a specific subscription."
    )
    @APIResponse(
            responseCode = "200",
            description = "Subscription details",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SubscriptionResponse.class)
            )
    )
    @APIResponse(responseCode = "404", description = "Subscription not found")
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    public Response getSubscription(
            @PathParam("subscriptionId")
            @Parameter(
                    description = "Unique subscription identifier (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            ) UUID subscriptionId
    ) {
        log.info("Subscription GET - user: {}, subscriptionId: {}", jwt.getSubject(), subscriptionId);
        return Response.ok(mapper.toResponse(subscriptionService.getSubscription(subscriptionId))).build();
    }

    @PUT
    @Operation(
            operationId = "updateSubscriptionStatus",
            summary = "Update subscription status",
            description = "Allows a service consumer to pause or resume a subscription. PAUSED: Stops message delivery without deleting the subscription. ACTIVE: Resumes message delivery. DELETED: Marks subscription for deletion."
    )
    @APIResponse(
            responseCode = "200",
            description = "Subscription status updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SubscriptionResponse.class)
            )
    )
    @APIResponse(responseCode = "404", description = "Subscription not found")
    @APIResponse(responseCode = "400", description = "Invalid status value")
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    public Response updateSubscriptionStatus(
            @PathParam("subscriptionId")
            @Parameter(
                    description = "Unique subscription identifier (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            ) UUID subscriptionId,
            @Valid @RequestBody(
                    description = "Status update payload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SubscriptionStatusUpdate.class),
                            examples = {
                                    @ExampleObject(name = "activate", summary = "Activate subscription", value = "{\"subscription_status\": \"ACTIVE\"}"),
                                    @ExampleObject(name = "pause", summary = "Pause subscription", value = "{\"subscription_status\": \"PAUSED\"}"),
                                    @ExampleObject(name = "cancel", summary = "Cancel subscription", value = "{\"subscription_status\": \"DELETED\"}")
                            }
                    )
            ) SubscriptionStatusUpdate statusUpdate
    ) {
        log.info("Subscription UPDATE - user: {}, subscriptionId: {}, newStatus: {}",
                jwt.getSubject(), subscriptionId, statusUpdate.subscriptionStatus());
        return Response.ok(mapper.toResponse(subscriptionService.updateStatus(subscriptionId, statusUpdate.subscriptionStatus()))).build();
    }

    @PUT
    @Path("/renew")
    @Operation(
            operationId = "renewSubscription",
            summary = "Renew subscription by extending subscriptionEnd",
            description = """
                    SWIM specification requirement: Consumer responsibility to renew subscriptions before expiration.

                    Extends the subscription termination time (subscriptionEnd) by the default TTL (24h) from now.
                    This prevents automatic termination by the expiry scheduler.

                    **Status Requirements**: Subscription must be in ACTIVE or PAUSED status to be renewed.
                    """
    )
    @APIResponse(
            responseCode = "200",
            description = "Subscription renewed successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SubscriptionResponse.class)
            )
    )
    @APIResponse(responseCode = "404", description = "Subscription not found")
    @APIResponse(responseCode = "400", description = "Subscription cannot be renewed (invalid status)")
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    public Response renewSubscription(
            @PathParam("subscriptionId")
            @Parameter(
                    description = "Unique subscription identifier (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            ) UUID subscriptionId
    ) {
        log.info("Subscription RENEW - user: {}, subscriptionId: {}", jwt.getSubject(), subscriptionId);
        return Response.ok(mapper.toResponse(subscriptionService.renewSubscription(subscriptionId, null))).build();
    }

    @DELETE
    @Operation(
            operationId = "unsubscribe",
            summary = "Delete subscription",
            description = "Unsubscribes from event scenarios and deletes the subscription permanently. The associated AMQP queue will be deleted, and the subscriber will no longer receive messages for this subscription."
    )
    @APIResponse(responseCode = "204", description = "Subscription deleted successfully")
    @APIResponse(responseCode = "404", description = "Subscription not found")
    @APIResponse(responseCode = "401", description = "Authentication failed - invalid or missing client certificate")
    public Response unsubscribe(
            @PathParam("subscriptionId")
            @Parameter(
                    description = "Unique subscription identifier (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            ) UUID subscriptionId
    ) {
        log.info("Subscription DELETE - user: {}, subscriptionId: {}", jwt.getSubject(), subscriptionId);
        subscriptionService.deleteSubscription(subscriptionId);
        return Response.noContent().build();
    }
}
