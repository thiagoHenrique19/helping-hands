package com.helpinghands.application.service;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.exception.AuthenticationFailedException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.port.in.AuthenticateUserUseCase;
import com.helpinghands.core.port.in.BootstrapAdminUseCase;
import com.helpinghands.core.port.in.RegisterUserUseCase;
import com.helpinghands.core.port.in.dto.LoginRequest;
import com.helpinghands.core.port.in.dto.RegisterUserRequest;
import com.helpinghands.core.port.in.dto.TokenResponse;
import com.helpinghands.core.port.in.dto.UserResponse;
import com.helpinghands.core.port.out.PasswordHasher;
import com.helpinghands.core.port.out.TokenIssuer;
import com.helpinghands.core.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements RegisterUserUseCase, AuthenticateUserUseCase, BootstrapAdminUseCase {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new InvalidRequestException("Only VOLUNTEER and ORGANIZATION accounts can be registered");
        }
        return toResponse(create(request.email(), request.password(), request.role()));
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(Organization.normalizeEmail(request.email()))
                .filter(User::isActive)
                .filter(found -> passwordHasher.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        var token = tokenIssuer.issue(user);
        return TokenResponse.bearer(token.value(), token.expiresInSeconds());
    }

    @Override
    @Transactional
    public boolean bootstrapAdmin(String email, String password) {
        if (userRepository.count() > 0) {
            return false;
        }
        create(email, password, Role.ADMIN);
        return true;
    }

    private User create(String email, String password, Role role) {
        String normalized = Organization.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new ConflictException("A user with email " + normalized + " already exists");
        }
        return userRepository.save(User.register(normalized, passwordHasher.hash(password), role));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getOrganizationId(),
                user.isActive());
    }
}
