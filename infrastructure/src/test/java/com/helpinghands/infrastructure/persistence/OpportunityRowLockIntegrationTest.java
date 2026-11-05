package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OpportunityRowLockIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private VolunteerOpportunityRepository opportunities;
    @Autowired private OrganizationRepository organizations;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private Long newOpportunity(int slots) {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        return opportunities.save(VolunteerOpportunity.create(organization, "Task", "d", "Somewhere", false,
                startsAt, startsAt.plus(2, ChronoUnit.HOURS), slots)).getId();
    }

    @Test
    void shouldBlockSecondWriterUntilFirstTransactionEnds() throws Exception {
        Long id = newOpportunity(3);
        var transactions = new TransactionTemplate(transactionManager);
        var secondStarted = new CountDownLatch(1);
        var secondGotLock = new AtomicBoolean(false);
        var second = new CompletableFuture<Void>();

        transactions.executeWithoutResult(status -> {
            opportunities.findByIdForUpdate(id).orElseThrow();
            CompletableFuture.runAsync(() -> {
                secondStarted.countDown();
                transactions.executeWithoutResult(inner -> {
                    opportunities.findByIdForUpdate(id).orElseThrow();
                    secondGotLock.set(true);
                });
            }).whenComplete((ok, error) -> second.complete(null));
            try {
                secondStarted.await(5, TimeUnit.SECONDS);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertThat(secondGotLock).isFalse();
        });

        second.get(10, TimeUnit.SECONDS);
        assertThat(secondGotLock).isTrue();
    }

    @Test
    void shouldReadFreshStateEvenWhenTheRowIsAlreadyInThePersistenceContext() {
        Long id = newOpportunity(3);
        var transactions = new TransactionTemplate(transactionManager);

        int filled = transactions.execute(status -> {
            opportunities.findById(id).orElseThrow();
            jdbc.update("UPDATE volunteer_opportunity SET filled_slots = 2 WHERE id = ?", id);
            return opportunities.findByIdForUpdate(id).orElseThrow().getFilledSlots();
        });

        assertThat(filled).isEqualTo(2);
    }
}
