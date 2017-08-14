package io.vash.example.spring.security.auth.basic.service;

import lombok.Value;

import java.security.Principal;

public interface AuthenticationJournal {

    AuthenticationLogEntry success(final Principal principal);

    @Value
    class AuthenticationLogEntry {
        Principal principal;
        AuthenticationState state;
        long utc;
    }

    enum AuthenticationState {
        SUCCESSFUL
    }
}