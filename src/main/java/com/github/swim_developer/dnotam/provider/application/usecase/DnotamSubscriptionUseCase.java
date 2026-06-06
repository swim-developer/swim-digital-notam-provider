package com.github.swim_developer.dnotam.provider.application.usecase;

import com.github.swim_developer.dnotam.provider.application.port.in.DnotamSubscriptionConfig;
import com.github.swim_developer.dnotam.provider.application.port.out.DnotamSubscriptionHashPort;
import com.github.swim_developer.dnotam.provider.application.port.out.DnotamSubscriptionMappingPort;
import com.github.swim_developer.dnotam.provider.domain.model.Subscription;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionResult;
import com.github.swim_developer.dnotam.provider.application.port.in.ManageSubscriptionPort;
import com.github.swim_developer.dnotam.provider.application.port.out.SubscriptionStore;
import com.github.swim_developer.framework.application.port.out.SwimSubscriptionQueuePort;
import com.github.swim_developer.framework.provider.application.subscription.AbstractProviderSubscriptionService;
import com.github.swim_developer.framework.provider.application.subscription.TopicService;
import com.github.swim_developer.framework.application.port.out.SwimSecurityContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;


@ApplicationScoped
@Slf4j
public class DnotamSubscriptionUseCase
        extends AbstractProviderSubscriptionService<Subscription, SubscriptionCommand, SubscriptionResult>
        implements ManageSubscriptionPort {

    private static final String QUEUE_PREFIX = "DNOTAM-";

    private final DnotamSubscriptionMappingPort subscriptionMapper;
    private final TopicService topicService;
    private final DnotamSubscriptionHashPort hashCalculator;
    private final DnotamSubscriptionConfig config;

    @Inject
    protected DnotamSubscriptionUseCase(SwimSecurityContext securityContext,
                                        SwimSubscriptionQueuePort queueOrchestrator,
                                        SubscriptionStore subscriptionRepository,
                                        DnotamSubscriptionMappingPort subscriptionMapper,
                                        TopicService topicService,
                                        DnotamSubscriptionHashPort hashCalculator,
                                        DnotamSubscriptionConfig config) {
        super(securityContext, queueOrchestrator, subscriptionRepository);
        this.subscriptionMapper = subscriptionMapper;
        this.topicService = topicService;
        this.hashCalculator = hashCalculator;
        this.config = config;
    }

    protected DnotamSubscriptionUseCase() {
        this(null, null, null, null, null, null, null);
    }


    @Override
    protected String getQueuePrefix() {
        return QUEUE_PREFIX;
    }

    @Override
    protected Duration getDefaultTtl() {
        return config.defaultTtl();
    }

    @Override
    protected String getRequestedQueueName(SubscriptionCommand command) {
        return command.queueName();
    }

    @Override
    protected String calculateHash(SubscriptionCommand command, String userId) {
        return hashCalculator.calculateHash(command, userId);
    }

    @Override
    protected Subscription createEntity(SubscriptionCommand command, String userId, String queueName, String hash) {
        return subscriptionMapper.toEntity(command, userId, hash, queueName);
    }

    @Override
    protected SubscriptionResult mapToResponse(Subscription entity) {
        return subscriptionMapper.toResponse(entity);
    }

    @Override
    protected void validateRequest(SubscriptionCommand command, String userId) {
        try {
            topicService.getTopic(command.topic());
        } catch (NotFoundException e) {
            log.warn("Invalid topic requested: {}", command.topic());
            throw new BadRequestException("Topic not available: " + command.topic() +
                    ". Use GET /swim/v1/topics to see available topics.");
        }
    }
}
