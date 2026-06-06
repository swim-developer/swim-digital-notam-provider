package com.github.swim_developer.dnotam.provider.infrastructure.config;

import com.github.swim_developer.dnotam.provider.application.port.in.DnotamSubscriptionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class QuarkusDnotamSubscriptionConfig implements DnotamSubscriptionConfig {

    private final Duration defaultTtl;

    @Inject
    public QuarkusDnotamSubscriptionConfig(
            @ConfigProperty(name = "swim.subscription.expiry.default-ttl", defaultValue = "24h")
            Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    @Override
    public Duration defaultTtl() {
        return defaultTtl;
    }
}
