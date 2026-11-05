package com.helpinghands.infrastructure.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacksonIdempotencyPayloadsTest {

    private final JacksonIdempotencyPayloads payloads =
            new JacksonIdempotencyPayloads(new ObjectMapper().registerModule(new JavaTimeModule()));

    private DonationRequest donation(String amount, String email) {
        return new DonationRequest("Ana", email, new BigDecimal(amount), false, "hi", PaymentMethod.PIX);
    }

    @Test
    void shouldProduceSha256HexDigest() {
        assertThat(payloads.fingerprint(donation("10", "ana@mail.com"))).matches("[0-9a-f]{64}");
    }

    @Test
    void shouldGiveTheSameFingerprintForTheSameContent() {
        assertThat(payloads.fingerprint(donation("10", "ana@mail.com")))
                .isEqualTo(payloads.fingerprint(donation("10", "ana@mail.com")));
    }

    @Test
    void shouldTreatEquivalentDecimalsAsEqual() {
        assertThat(payloads.fingerprint(donation("10", "ana@mail.com")))
                .isEqualTo(payloads.fingerprint(donation("10.00", "ana@mail.com")));
    }

    @Test
    void shouldNotConfuseTenWithOneHundred() {
        assertThat(payloads.fingerprint(donation("10", "ana@mail.com")))
                .isNotEqualTo(payloads.fingerprint(donation("100", "ana@mail.com")));
    }

    @Test
    void shouldChangeWhenAnyFieldChanges() {
        assertThat(payloads.fingerprint(donation("10", "ana@mail.com")))
                .isNotEqualTo(payloads.fingerprint(donation("10", "bob@mail.com")));
    }

    @Test
    void shouldIgnoreFieldOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", 2);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("b", 2);
        second.put("a", 1);

        assertThat(payloads.fingerprint(first)).isEqualTo(payloads.fingerprint(second));
    }

    @Test
    void shouldRoundTripAResponseIncludingInstantsAndDecimals() {
        var response = new DonationResponse(7L, 3L, "Ana", new BigDecimal("12.50"),
                Instant.parse("2026-01-02T03:04:05Z"), "DON-00000007", false, "hi", PaymentMethod.PIX);

        String snapshot = payloads.serialize(response);

        assertThat(payloads.deserialize(snapshot, DonationResponse.class)).isEqualTo(response);
    }
}
