package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.User;

public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
