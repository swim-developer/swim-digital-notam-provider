package com.github.swim_developer.dnotam.provider.application.port.in;

import com.github.swim_developer.dnotam.provider.domain.model.EventQueryFilters;

import java.util.Optional;

public interface QueryEventPort {

    String queryFeatures(EventQueryFilters filters);

    Optional<String> findByEventId(String eventId);
}
