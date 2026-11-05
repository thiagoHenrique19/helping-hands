package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.port.out.ReceiptCodeGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SequenceReceiptCodeGenerator implements ReceiptCodeGenerator {

    private final JdbcTemplate jdbcTemplate;

    SequenceReceiptCodeGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String next() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('donation_receipt_seq')", Long.class);
        return String.format("DON-%08d", value);
    }
}
