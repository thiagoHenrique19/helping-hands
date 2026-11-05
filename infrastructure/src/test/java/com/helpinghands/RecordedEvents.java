package com.helpinghands;

import com.helpinghands.core.event.DomainEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.event.EventListener;

@TestComponent
public class RecordedEvents {

    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    void on(DomainEvent event) {
        events.add(event);
    }

    public <E extends DomainEvent> List<E> of(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
