package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.Role;
import com.helpinghands.core.exception.AuthenticationFailedException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.port.in.AuthenticateUserUseCase;
import com.helpinghands.core.port.in.RegisterUserUseCase;
import com.helpinghands.core.port.in.dto.TokenResponse;
import com.helpinghands.core.port.in.dto.UserResponse;
import com.helpinghands.infrastructure.security.WebMvcSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(WebMvcSecurityTestConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RegisterUserUseCase registerUser;
    @MockBean private AuthenticateUserUseCase authenticateUser;

    @Test
    void shouldRegisterWithoutAuthentication() throws Exception {
        when(registerUser.register(any())).thenReturn(new UserResponse(1L, "jane@mail.com", Role.VOLUNTEER, null, true));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@mail.com\",\"password\":\"secret-pass\",\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@mail.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldValidateRegistrationPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        when(registerUser.register(any())).thenThrow(new ConflictException("exists"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@mail.com\",\"password\":\"secret-pass\",\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginAndReturnBearerToken() throws Exception {
        when(authenticateUser.login(any())).thenReturn(TokenResponse.bearer("jwt", 3600));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@mail.com\",\"password\":\"secret-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void shouldReturnUnauthorizedForBadCredentials() throws Exception {
        when(authenticateUser.login(any())).thenThrow(new AuthenticationFailedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@mail.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }
}
