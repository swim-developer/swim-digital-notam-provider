package com.github.swim_developer.dnotam.provider.application.port.out;

import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;

import java.util.List;

public interface AixmMessageAssemblerPort {

    String assemble(List<DnotamStoredEvent> events);
}
