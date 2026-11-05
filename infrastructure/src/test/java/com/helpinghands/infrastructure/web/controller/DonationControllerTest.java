package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ListDonationsUseCase;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.ViewTopDonorsUseCase;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.helpinghands.infrastructure.security.WebMvcSecurityTestConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DonationController.class)
@Import(WebMvcSecurityTestConfig.class)
class DonationControllerTest {

    private static final String VALID_BODY = """
            {"donorName":"Alice","donorEmail":"alice@mail.com","amount":40,"anonymous":true,
             "message":"Good luck","paymentMethod":"PIX"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private MakeDonationUseCase donationService;
    @MockBean private ListDonationsUseCase listDonations;
    @MockBean private ViewTopDonorsUseCase viewTopDonors;

    @Test
    void shouldReturnCreatedWhenDonationIsValid() throws Exception {
        when(donationService.donate(eq(1L), any(), any())).thenReturn(new DonationResponse(10L, 1L, "Alice",
                new BigDecimal("40"), Instant.now(), "DON-00000010", true, "Good luck", PaymentMethod.PIX));

        mockMvc.perform(post("/api/campaigns/1/donations")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorName").value("Alice"))
                .andExpect(jsonPath("$.receiptCode").value("DON-00000010"))
                .andExpect(jsonPath("$.anonymous").value(true));
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
    void shouldReturnBadRequestWhenPaymentMethodIsMissingOrUnknown() throws Exception {
        mockMvc.perform(post("/api/campaigns/1/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"A\",\"donorEmail\":\"a@a.com\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/campaigns/1/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"A\",\"donorEmail\":\"a@a.com\",\"amount\":10,\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityOnBusinessRuleViolation() throws Exception {
        when(donationService.donate(eq(1L), any(), any())).thenThrow(new BusinessRuleException("Campaign is not active"));

        mockMvc.perform(post("/api/campaigns/1/donations")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Campaign is not active"));
    }

    @Test
    void shouldListPublicDonationsNewestFirstByDefault() throws Exception {
        when(listDonations.listByCampaign(1L, new PageQuery(0, 20, "createdAt", SortDirection.DESC)))
                .thenReturn(new PageResult<>(List.of(
                        new PublicDonationResponse(1L, "Anonymous", BigDecimal.TEN, "hi", Instant.now())), 0, 20, 1, 1));

        mockMvc.perform(get("/api/campaigns/1/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].donorName").value("Anonymous"))
                .andExpect(jsonPath("$.content[0].donorEmail").doesNotExist());
    }

    @Test
    void shouldReturnTopDonors() throws Exception {
        when(viewTopDonors.topDonors(1L, 3)).thenReturn(List.of(new TopDonorResponse("Alice", new BigDecimal("80"), 2)));

        mockMvc.perform(get("/api/campaigns/1/top-donors?limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donorName").value("Alice"))
                .andExpect(jsonPath("$[0].donationsCount").value(2));
    }

    @Test
    void shouldReturnBadRequestForInvalidTopDonorsLimit() throws Exception {
        when(viewTopDonors.topDonors(1L, 500)).thenThrow(new InvalidRequestException("Limit must be between 1 and 50"));

        mockMvc.perform(get("/api/campaigns/1/top-donors?limit=500")).andExpect(status().isBadRequest());
    }
}
