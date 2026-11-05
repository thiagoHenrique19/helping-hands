package com.helpinghands.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdempotencyKeyTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "3f2b8c1e-9d4a-4c55-8a51-0d6f4f0a1b2c", "order_42:retry-1", "A.B~C"})
    void shouldAcceptPrintableKeys(String value) {
        assertThat(IdempotencyKey.of(value).value()).isEqualTo(value);
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        assertThat(IdempotencyKey.of("  abc  ").value()).isEqualTo("abc");
    }

    @Test
    void shouldAcceptKeyOfMaximumLength() {
        assertThat(IdempotencyKey.of("k".repeat(IdempotencyKey.MAX_LENGTH)).value()).hasSize(255);
    }

    @Test
    void shouldRejectKeyOneCharacterOverTheLimit() {
        assertThatThrownBy(() -> IdempotencyKey.of("k".repeat(IdempotencyKey.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("255");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "has space", "tab\tinside", "line\nbreak", "accént", "emoji🙂"})
    void shouldRejectBlankOrNonPrintableKeys(String value) {
        assertThatThrownBy(() -> IdempotencyKey.of(value)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldBuildScopedKeyPerResource() {
        assertThat(IdempotencyKey.of("abc").scopedTo("donation", 7L)).isEqualTo("donation:7");
    }
}
