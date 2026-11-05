package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.exception.AuthenticationFailedException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.port.in.dto.LoginRequest;
import com.helpinghands.core.port.in.dto.RegisterUserRequest;
import com.helpinghands.core.port.in.dto.TokenResponse;
import com.helpinghands.core.port.in.dto.UserResponse;
import com.helpinghands.core.port.out.PasswordHasher;
import com.helpinghands.core.port.out.TokenIssuer;
import com.helpinghands.core.port.out.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuer tokenIssuer;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordHasher, tokenIssuer);
    }

    @Test
    void shouldRegisterVolunteerWithHashedPassword() {
        when(userRepository.existsByEmail("jane@mail.com")).thenReturn(false);
        when(passwordHasher.hash("secret-pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = service.register(new RegisterUserRequest("Jane@Mail.com", "secret-pass", Role.VOLUNTEER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(response.email()).isEqualTo("jane@mail.com");
        assertThat(response.role()).isEqualTo(Role.VOLUNTEER);
    }

    @Test
    void shouldRegisterOrganizationUser() {
        when(userRepository.existsByEmail("ngo@mail.com")).thenReturn(false);
        when(passwordHasher.hash("secret-pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.register(new RegisterUserRequest("ngo@mail.com", "secret-pass", Role.ORGANIZATION)).role())
                .isEqualTo(Role.ORGANIZATION);
    }

    @Test
    void shouldRefuseSelfRegisteredAdmin() {
        assertThatThrownBy(() -> service.register(new RegisterUserRequest("a@mail.com", "secret-pass", Role.ADMIN)))
                .isInstanceOf(InvalidRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("jane@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterUserRequest("jane@mail.com", "secret-pass", Role.VOLUNTEER)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldLoginWithValidCredentials() {
        User user = User.register("jane@mail.com", "hashed", Role.VOLUNTEER);
        when(userRepository.findByEmail("jane@mail.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret-pass", "hashed")).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(new TokenIssuer.IssuedToken("jwt-token", 3600));

        TokenResponse token = service.login(new LoginRequest("JANE@mail.com", "secret-pass"));

        assertThat(token.accessToken()).isEqualTo("jwt-token");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(3600);
    }

    @Test
    void shouldRejectWrongPasswordUnknownEmailAndInactiveUser() {
        User user = User.register("jane@mail.com", "hashed", Role.VOLUNTEER);
        when(userRepository.findByEmail("jane@mail.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "hashed")).thenReturn(false);
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("jane@mail.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@mail.com", "whatever")))
                .isInstanceOf(AuthenticationFailedException.class);

        user.deactivate();
        assertThatThrownBy(() -> service.login(new LoginRequest("jane@mail.com", "secret-pass")))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void shouldBootstrapAdminOnlyWhenNoUsersExist() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordHasher.hash("admin-pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.bootstrapAdmin("admin@mail.com", "admin-pass")).isTrue();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldSkipBootstrapWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(3L);

        assertThat(service.bootstrapAdmin("admin@mail.com", "admin-pass")).isFalse();
        verify(userRepository, never()).save(any());
    }
}
