package com.github.swim_developer.dnotam.provider.infrastructure.in.rest;

import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.TopicsResponse;
import com.github.swim_developer.framework.provider.application.subscription.TopicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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

import java.util.Map;

@Path("/swim/v1/topics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
@Tag(name = "Topics", description = "Available services for subscription")
@Slf4j
public class TopicResource {

    private final TopicService topicService;

    @Inject
    public TopicResource(TopicService topicService) {
        this.topicService = topicService;
    }

    @GET
    @Operation(
            operationId = "getTopics",
            summary = "Get list of available topics",
            description = "Returns the list of topics (service names) available for subscription."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of available topics",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TopicsResponse.class)
            )
    )
    @APIResponse(
            responseCode = "401",
            description = "Authentication failed"
    )
    public Response getTopics() {
        log.debug("Retrieving all topics");
        TopicsResponse response = new TopicsResponse(topicService.getAllTopics());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{topicId}")
    @Operation(
            operationId = "getTopic",
            summary = "Get topic",
            description = "Returns confirmation that the topic exists and is available for subscription."
    )
    @APIResponse(
            responseCode = "200",
            description = "Topic exists",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Topic not found"
    )
    @APIResponse(
            responseCode = "401",
            description = "Authentication failed"
    )
    public Response getTopic(
            @PathParam("topicId")
            @Parameter(
                    description = "Topic identifier (service name)",
                    required = true,
                    example = "DigitalNOTAMService"
            ) String topicId
    ) {
        log.debug("Retrieving topic: {}", topicId);
        String topic = topicService.getTopic(topicId);
        return Response.ok(Map.of("topic", topic)).build();
    }
}
