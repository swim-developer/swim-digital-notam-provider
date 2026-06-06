package com.github.swim_developer.dnotam.provider.application.port.in;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.framework.domain.model.DeliveryResult;

public interface DeliverEventPort {

    DeliveryResult deliverToMatchingSubscriptions(DnotamStoredEvent event);
}
