package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.LoginRequest;
import com.helpinghands.core.port.in.dto.TokenResponse;

public interface AuthenticateUserUseCase {

    TokenResponse login(LoginRequest request);
}
