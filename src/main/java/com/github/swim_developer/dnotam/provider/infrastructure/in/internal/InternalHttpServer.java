package com.github.swim_developer.dnotam.provider.infrastructure.in.internal;

import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler.InternalStatusHandler;
import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler.InternalTriggerHandler;
import com.github.swim_developer.dnotam.provider.infrastructure.in.internal.handler.InternalValidateHandler;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
@Slf4j
public class InternalHttpServer {

    private final Vertx vertx;
    private final InternalTriggerHandler triggerHandler;
    private final InternalValidateHandler validateHandler;
    private final InternalStatusHandler statusHandler;
    private final int port;

    private HttpServer server;

    @Inject
    public InternalHttpServer(Vertx vertx,
                              InternalTriggerHandler triggerHandler,
                              InternalValidateHandler validateHandler,
                              InternalStatusHandler statusHandler,
                              @ConfigProperty(name = "internal.server.port", defaultValue = "9080") int port) {
        this.vertx = vertx;
        this.triggerHandler = triggerHandler;
        this.validateHandler = validateHandler;
        this.statusHandler = statusHandler;
        this.port = port;
    }

    void onStart(@Observes StartupEvent ev) {
        Router router = Router.router(vertx.getDelegate());
        router.route().handler(BodyHandler.create());

        router.post("/internal/v1/trigger").handler(triggerHandler::handle);
        router.post("/internal/v1/validate").handler(validateHandler::handle);
        router.get("/internal/v1/subscriptions/summary").handler(statusHandler::handleSubscriptionsSummary);
        router.get("/internal/v1/status").handler(statusHandler::handleStatus);
        router.get("/internal/v1/openapi.yaml").handler(this::handleOpenApiSpec);

        server = vertx.getDelegate().createHttpServer()
                .requestHandler(router)
                .listen(port)
                .result();

        log.info("Internal HTTP server started on port {}", port);
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (server != null) {
            server.close();
            log.info("Internal HTTP server stopped");
        }
    }

    private void handleOpenApiSpec(io.vertx.ext.web.RoutingContext ctx) {
        try (InputStream is = getClass().getResourceAsStream("/internal/openapi.yaml")) {
            if (is == null) {
                InternalResponseHelper.sendError(ctx, 404, "OpenAPI spec not found");
                return;
            }
            String spec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            ctx.response()
                    .putHeader("Content-Type", "application/yaml")
                    .end(spec);
        } catch (Exception e) {
            InternalResponseHelper.sendError(ctx, 500, e.getMessage());
        }
    }
}
