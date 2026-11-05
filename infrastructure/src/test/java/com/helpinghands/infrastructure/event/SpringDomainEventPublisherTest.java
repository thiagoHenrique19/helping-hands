package com.helpinghands.infrastructure.event;

import static org.mockito.Mockito.verify;

import com.helpinghands.core.event.OpportunityFilled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void shouldForwardDomainEventsToSpring() {
        var event = new OpportunityFilled(1L, 2L);

        new SpringDomainEventPublisher(applicationEventPublisher).publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
