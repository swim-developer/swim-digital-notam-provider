package com.github.swim_developer.dnotam.provider.application.port.out;

import com.github.swim_developer.dnotam.provider.domain.model.SubscriptionCommand;

public interface DnotamSubscriptionHashPort {

    String calculateHash(SubscriptionCommand command, String userId);
}
