package io.vash.example.spring.security.auth.basic.service;

import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal.AuthenticationLogEntry;
import io.vavr.collection.List;

import java.security.Principal;

public interface AuthenticationInfo {
    List<AuthenticationLogEntry> successfulAttempts(final Principal principal, int limit);
}
