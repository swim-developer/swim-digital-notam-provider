package com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler;

import com.github.swim_developer.dnotam.provider.infrastructure.in.amqp.DnotamIngressMessageHandler;
import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.InternalResponseHelper;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class InternalTriggerHandler {

    private final Vertx vertx;
    private final DnotamIngressMessageHandler eventProcessor;

    @Inject
    public InternalTriggerHandler(Vertx vertx, DnotamIngressMessageHandler eventProcessor) {
        this.vertx = vertx;
        this.eventProcessor = eventProcessor;
    }

    public void handle(RoutingContext ctx) {
        String aixmMessage = ctx.body().asString();

        if (aixmMessage == null || aixmMessage.isBlank()) {
            InternalResponseHelper.sendError(ctx, 400, "Empty or missing XML body");
            return;
        }

        io.vertx.core.Vertx core = vertx.getDelegate();
        core.getOrCreateContext().executeBlocking(() -> {
            eventProcessor.processEvent(aixmMessage);
            return null;
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                InternalResponseHelper.sendSuccess(ctx, 202, "Event accepted for processing");
            } else {
                Throwable cause = ar.cause();
                log.error("Error processing triggered event", cause);
                String msg = cause != null && cause.getMessage() != null ? cause.getMessage() : "Internal error";
                InternalResponseHelper.sendError(ctx, 500, msg);
            }
        });
    }
}
