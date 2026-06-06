package com.github.swim_developer.dnotam.provider.infrastructure.out.subscription;

import com.github.swim_developer.dnotam.provider.application.port.out.DnotamSubscriptionHashPort;
import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;
import com.github.swim_developer.framework.application.service.AbstractSubscriptionHashCalculator;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class DnotamSubscriptionHashCalculator extends AbstractSubscriptionHashCalculator<SubscriptionCommand>
        implements DnotamSubscriptionHashPort {

    @Override
    public String calculateHash(SubscriptionCommand command, String userId) {
        StringBuilder sb = new StringBuilder();

        sb.append("userId:").append(userId).append("|");
        sb.append("topic:").append(command.topic()).append("|");
        sb.append("eventScenario:").append(sortedListToString(command.eventScenario())).append("|");
        sb.append("airportHeliport:").append(sortedListToString(command.airportHeliport())).append("|");
        sb.append("airspace:").append(sortedListToString(command.airspace())).append("|");
        sb.append("eventSeries:").append(nullSafe(command.eventSeries())).append("|");
        sb.append("publisher:").append(nullSafe(command.publisher())).append("|");
        sb.append("provider:").append(nullSafe(command.provider()));

        String data = sb.toString();
        log.debug("Calculating hash for: {}", data);

        return sha256(data);
    }
}
