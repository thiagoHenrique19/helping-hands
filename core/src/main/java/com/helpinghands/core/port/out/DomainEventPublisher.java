package com.helpinghands.core.port.out;

import com.helpinghands.core.event.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
