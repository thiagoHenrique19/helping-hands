package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CampaignRowLockIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void shouldMakeSecondWriterWaitUntilFirstTransactionEnds() throws Exception {
        var organization = organizationRepository.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        Long id = campaignRepository.save(Campaign.create(organization, "Lock me", "d", new BigDecimal("100"))).getId();
        var transactions = new TransactionTemplate(transactionManager);
        var secondStarted = new CountDownLatch(1);
        var secondGotLock = new AtomicBoolean(false);

        CompletableFuture<Void> second = new CompletableFuture<>();
        transactions.executeWithoutResult(status -> {
            campaignRepository.findByIdForUpdate(id).orElseThrow();
            CompletableFuture.runAsync(() -> {
                secondStarted.countDown();
                transactions.executeWithoutResult(inner -> {
                    campaignRepository.findByIdForUpdate(id).orElseThrow();
                    secondGotLock.set(true);
                });
            }).whenComplete((ok, error) -> second.complete(null));
            await(secondStarted);
            sleep(500);
            assertThat(secondGotLock).as("second writer must be blocked by the row lock").isFalse();
        });

        second.get(10, TimeUnit.SECONDS);
        assertThat(secondGotLock).isTrue();
    }

    @Test
    void shouldReturnEmptyForUnknownCampaign() {
        var found = new TransactionTemplate(transactionManager).execute(status -> campaignRepository.findByIdForUpdate(-1L));
        assertThat(found).isEmpty();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
