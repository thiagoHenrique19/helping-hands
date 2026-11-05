package com.helpinghands.infrastructure.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.IdempotencyKeyReusedException;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MDC.clear();
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldMapNotFoundToProblemDetail() throws Exception {
        mockMvc.perform(get("/fail/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Campaign not found with id 7"))
                .andExpect(jsonPath("$.instance").value("/fail/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldMapLockTimeoutToConflict() throws Exception {
        mockMvc.perform(get("/fail/lock-timeout"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The resource was modified concurrently, please retry"));
    }

    @Test
    void shouldMapIdempotencyKeyReuseToUnprocessableEntityWithOwnType() throws Exception {
        mockMvc.perform(get("/fail/key-reuse"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:idempotency-key-reuse"))
                .andExpect(jsonPath("$.title").value("Idempotency key reused"));
    }

    @Test
    void shouldMapDeadlockToConflict() throws Exception {
        mockMvc.perform(get("/fail/deadlock"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldMapBusinessRuleToUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/fail/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violation"))
                .andExpect(jsonPath("$.detail").value("Campaign is not active"));
    }

    @Test
    void shouldMapConflictsToConflict() throws Exception {
        mockMvc.perform(get("/fail/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already used"));
        mockMvc.perform(get("/fail/integrity")).andExpect(status().isConflict());
        mockMvc.perform(get("/fail/locking")).andExpect(status().isConflict());
    }

    @Test
    void shouldMapInvalidRequestToBadRequest() throws Exception {
        mockMvc.perform(get("/fail/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Page size must be between 1 and 100"));
    }

    @Test
    void shouldListFieldErrorsForValidationFailures() throws Exception {
        mockMvc.perform(post("/fail/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field=='name')].message").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='email')].message").exists());
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/fail/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnBadRequestForMissingParameterAndTypeMismatch() throws Exception {
        mockMvc.perform(get("/fail/param")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required parameter 'count' is not present."));
        mockMvc.perform(get("/fail/param?count=abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'count'"));
    }

    @Test
    void shouldHideUnexpectedErrorDetails() throws Exception {
        mockMvc.perform(get("/fail/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void shouldIncludeRequestIdWhenPresent() throws Exception {
        MDC.put("requestId", "req-123");

        mockMvc.perform(get("/fail/not-found")).andExpect(jsonPath("$.requestId").value("req-123"));
    }

    @RestController
    static class FailingController {

        record Payload(@NotBlank String name, @NotBlank @Email String email) {
        }

        @GetMapping("/fail/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Campaign", 7L);
        }

        @GetMapping("/fail/business")
        void business() {
            throw new BusinessRuleException("Campaign is not active");
        }

        @GetMapping("/fail/conflict")
        void conflict() {
            throw new ConflictException("Email already used");
        }

        @GetMapping("/fail/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @GetMapping("/fail/locking")
        void locking() {
            throw new OptimisticLockingFailureException("stale");
        }

        @GetMapping("/fail/lock-timeout")
        void lockTimeout() {
            throw new CannotAcquireLockException("lock wait timeout");
        }

        @GetMapping("/fail/deadlock")
        void deadlock() {
            throw new DeadlockLoserDataAccessException("deadlock", null);
        }

        @GetMapping("/fail/key-reuse")
        void keyReuse() {
            throw new IdempotencyKeyReusedException();
        }

        @GetMapping("/fail/invalid")
        void invalid() {
            throw new InvalidRequestException("Page size must be between 1 and 100");
        }

        @PostMapping("/fail/validate")
        void validate(@Valid @RequestBody Payload payload) {
        }

        @GetMapping("/fail/param")
        void param(@RequestParam int count) {
        }

        @GetMapping("/fail/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
