package io.vash.example.spring.security.auth.basic.service;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.security.core.Authentication;

import java.security.Principal;

public interface AuthenticationVault {

    Authentication register(Principal identifier) throws IdentifierConflict;

    Authentication match(Authentication identifier) throws IdentifierMismatch;

    default SecurityException collision(Principal principal) {
        return new IdentifierConflict(principal);
    }

    default IdentifierMismatch mismatch(Principal principal) {
        return new IdentifierMismatch(principal);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    final class IdentifierConflict extends SecurityException {
        Principal identifier;

        IdentifierConflict(Principal identifier) {
            super("Identifier Conflict");
            this.identifier = identifier;
        }

        @Override
        public String getLocalizedMessage() {
            return String.format("Identifier [%s] was already taken",identifier.getName());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    final class IdentifierMismatch extends SecurityException {
        Principal principal;
    }
}