package com.helpinghands.core.port.in;

public interface BootstrapAdminUseCase {

    /**
     * Creates the first administrator, but only when no user exists yet.
     *
     * @return true when the administrator was created
     */
    boolean bootstrapAdmin(String email, String password);
}
