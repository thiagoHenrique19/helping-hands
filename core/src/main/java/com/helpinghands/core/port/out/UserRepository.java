package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long count();
}
