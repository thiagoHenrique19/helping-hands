package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.RegisterUserRequest;
import com.helpinghands.core.port.in.dto.UserResponse;

public interface RegisterUserUseCase {

    UserResponse register(RegisterUserRequest request);
}
