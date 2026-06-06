package com.github.swim_developer.dnotam.provider.infrastructure.out.mapper;

import com.github.swim_developer.dnotam.provider.application.port.out.DnotamSubscriptionMappingPort;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionResult;
import com.github.swim_developer.dnotam.provider.infrastructure.in.rest.dto.SubscriptionResponse;
import com.github.swim_developer.framework.domain.model.QualityOfService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class DnotamProviderSubscriptionMapper implements DnotamSubscriptionMappingPort {

    private static final String QUEUE_PREFIX = "DNOTAM";

    @ConfigProperty(name = "swim.subscription.expiry.default-ttl", defaultValue = "24h")
    Duration defaultTtl;

    @ConfigProperty(name = "swim.provider.name", defaultValue = "SWIM-DNOTAM-Provider")
    String providerName;

    public Subscription toEntity(SubscriptionCommand command, String userId, String subscriptionHash, String resolvedQueueName) {
        UUID subscriptionId = UUID.randomUUID();

        String queue = resolvedQueueName != null ? resolvedQueueName : generateQueue(userId, subscriptionId);

        QualityOfService qos = command.qos() != null ? command.qos() : QualityOfService.AT_LEAST_ONCE;
        Boolean durable = command.durable() != null ? command.durable() : Boolean.TRUE;

        Instant now = Instant.now();

        return Subscription.builder()
                .subscriptionId(subscriptionId)
                .topic(command.topic())
                .qos(qos)
                .durable(durable)
                .eventScenario(nullSafeList(command.eventScenario()))
                .airportHeliport(nullSafeList(command.airportHeliport()))
                .airspace(nullSafeList(command.airspace()))
                .eventSeries(command.eventSeries())
                .publisher(command.publisher())
                .provider(command.provider())
                .queue(queue)
                .userId(userId)
                .subscriptionHash(subscriptionHash)
                .description(command.description())
                .comment(command.comment())
                .createdAt(now)
                .updatedAt(now)
                .subscriptionEnd(now.plus(defaultTtl))
                .build();
    }

    public String generateQueueWithCustomName(String userId, String requestedQueueName) {
        if (requestedQueueName != null && !requestedQueueName.isBlank()) {
            return requestedQueueName;
        }
        return generateQueue(userId, UUID.randomUUID());
    }

    private List<String> nullSafeList(List<String> list) {
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private String generateQueue(String userId, UUID subscriptionId) {
        String queue = String.format("%s-%s-%s", QUEUE_PREFIX, userId, subscriptionId);
        log.debug("Generated queue: {}", queue);
        return queue;
    }

    public SubscriptionResult toResponse(Subscription subscription) {
        return new SubscriptionResult(
                subscription.getTopic(),
                subscription.getSubscriptionId(),
                subscription.getQueue(),
                subscription.getStatus(),
                subscription.getQos(),
                subscription.getDurable(),
                subscription.getSubscriptionEnd(),
                providerName,
                subscription.getQueue() + "-heartbeat",
                subscription.getEventScenario(),
                subscription.getAirportHeliport(),
                subscription.getAirspace(),
                subscription.getEventSeries(),
                subscription.getPublisher(),
                subscription.getDescription(),
                subscription.getComment()
        );
    }
}
