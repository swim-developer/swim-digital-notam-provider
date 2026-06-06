package com.github.swim_developer.unit;

import com.github.swim_developer.framework.provider.application.messaging.AfterCommitEventDispatcher;
import com.github.swim_developer.framework.infrastructure.out.cache.HandoffCache;
import com.github.swim_developer.framework.infrastructure.testing.TestNameLoggerExtension;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import jakarta.transaction.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(TestNameLoggerExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class AfterCommitEventDispatcherTest {

    private HandoffCache handoffCache;
    private Vertx vertx;
    private EventBus eventBus;
    private AfterCommitEventDispatcher dispatcher;
    private DnotamStoredEvent entity;

    @BeforeEach
    void setUp() {
        handoffCache = mock(HandoffCache.class);
        vertx = mock(Vertx.class);
        eventBus = mock(EventBus.class);
        when(vertx.eventBus()).thenReturn(eventBus);

        entity = DnotamStoredEvent.builder()
                .eventId("EVT-001")
                .eventScenario("RWY.CLS")
                .build();

        dispatcher = new AfterCommitEventDispatcher("EVT-001", entity, handoffCache, vertx, "outbox.deliver");
    }

    @Test
    void afterCompletionOnCommitCachesAndPublishes() {
        dispatcher.afterCompletion(Status.STATUS_COMMITTED);

        verify(handoffCache).put("EVT-001", entity);
        verify(eventBus).publish("outbox.deliver", "EVT-001");
    }

    @Test
    void afterCompletionOnRollbackDoesNotCacheOrPublish() {
        dispatcher.afterCompletion(Status.STATUS_ROLLEDBACK);

        verifyNoInteractions(handoffCache);
        verifyNoInteractions(eventBus);
    }

    @Test
    void afterCompletionOnUnknownStatusDoesNotCacheOrPublish() {
        dispatcher.afterCompletion(Status.STATUS_UNKNOWN);

        verifyNoInteractions(handoffCache);
        verifyNoInteractions(eventBus);
    }

    @Test
    void beforeCompletionDoesNothing() {
        dispatcher.beforeCompletion();

        verifyNoInteractions(handoffCache);
        verifyNoInteractions(vertx);
    }
}
