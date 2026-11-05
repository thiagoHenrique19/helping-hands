package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DonationController.class)
class DonationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MakeDonationUseCase donationService;

    @Test
    void shouldReturnCreatedWhenDonationIsValid() throws Exception {
        when(donationService.donate(eq(1L), any()))
                .thenReturn(new DonationResponse(10L, 1L, "Alice", new BigDecimal("40"), Instant.now()));

        mockMvc.perform(post("/api/campaigns/1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donorName":"Alice","donorEmail":"alice@mail.com","amount":40}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorName").value("Alice"));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/campaigns/1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donorName":"","donorEmail":"not-an-email","amount":-5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityOnBusinessRuleViolation() throws Exception {
        when(donationService.donate(eq(1L), any())).thenThrow(new BusinessRuleException("Campaign is not active"));

        mockMvc.perform(post("/api/campaigns/1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donorName":"Alice","donorEmail":"alice@mail.com","amount":40}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Campaign is not active"));
    }
}
