package com.github.swim_developer.dnotam.provider.application.port.in;

import java.time.Duration;

public interface DnotamSubscriptionConfig {
    Duration defaultTtl();
}
