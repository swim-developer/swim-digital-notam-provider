package com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler;

import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.InternalResponseHelper;
import com.github.swim_developer.dnotam.provider.infrastructure.out.xml.DnotamJaxbUnmarshallerPool;
import com.github.swim_developer.framework.domain.exception.XmlValidationException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InternalValidateHandler {

    private static final String VALID_KEY = "valid";
    private static final String MESSAGE_KEY = "message";

    private final Vertx vertx;
    private final DnotamJaxbUnmarshallerPool jaxbPool;

    @Inject
    public InternalValidateHandler(Vertx vertx, DnotamJaxbUnmarshallerPool jaxbPool) {
        this.vertx = vertx;
        this.jaxbPool = jaxbPool;
    }

    public void handle(RoutingContext ctx) {
        String aixmMessage = ctx.body().asString();

        if (aixmMessage == null || aixmMessage.isBlank()) {
            InternalResponseHelper.sendError(ctx, 400, "Empty or missing XML body");
            return;
        }

        io.vertx.core.Vertx core = vertx.getDelegate();
        core.getOrCreateContext().executeBlocking(() -> {
            try {
                jaxbPool.unmarshalAndValidate(aixmMessage);
                return new JsonObject()
                        .put(VALID_KEY, true)
                        .put(MESSAGE_KEY, "AIXM message is valid (JAXB unmarshal succeeded)");
            } catch (XmlValidationException e) {
                return new JsonObject()
                        .put(VALID_KEY, false)
                        .put(MESSAGE_KEY, e.getMessage());
            }
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                JsonObject result = ar.result();
                int status = Boolean.TRUE.equals(result.getBoolean(VALID_KEY)) ? 200 : 422;
                InternalResponseHelper.sendJson(ctx, status, result);
            } else {
                InternalResponseHelper.sendError(ctx, 500, ar.cause().getMessage());
            }
        });
    }
}
