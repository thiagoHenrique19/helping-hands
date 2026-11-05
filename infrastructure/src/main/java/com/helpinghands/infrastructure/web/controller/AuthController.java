package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.port.in.AuthenticateUserUseCase;
import com.helpinghands.core.port.in.RegisterUserUseCase;
import com.helpinghands.core.port.in.dto.LoginRequest;
import com.helpinghands.core.port.in.dto.RegisterUserRequest;
import com.helpinghands.core.port.in.dto.TokenResponse;
import com.helpinghands.core.port.in.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Account registration and login (stateless JWT)")
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;

    public AuthController(RegisterUserUseCase registerUser, AuthenticateUserUseCase authenticateUser) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
    }

    @Operation(summary = "Register a VOLUNTEER or ORGANIZATION account")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return registerUser.register(request);
    }

    @Operation(summary = "Log in and receive a bearer token")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticateUser.login(request);
    }
}
