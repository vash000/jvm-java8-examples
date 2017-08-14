package io.vash.example.spring.security.auth.basic.respository;

import io.vash.example.spring.security.auth.basic.service.AuthenticationInfo;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal.AuthenticationLogEntry;
import io.vavr.collection.List;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
class InMemoryUserVisitJournal  {

    static final HashMap<Principal,List<AuthenticationLogEntry>> successLog = new HashMap<>();

    static class SuccessAuthenticationJournalWriter implements AuthenticationInfo {
        @Override
        public List<AuthenticationLogEntry> successfulAttempts(final Principal principal, int limit) {
            return successLog.getOrDefault(principal,List.empty()).takeRight(limit);
        }
    }

    static class SuccessAuthenticationJournalReader implements AuthenticationJournal {

        private long timestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public AuthenticationLogEntry success(final Principal principal) {
            Objects.requireNonNull(principal,"identifier can not be null");
            return logSuccess(new AuthenticationLogEntry(principal, AuthenticationState.SUCCESSFUL,timestamp()));
        }

        private AuthenticationLogEntry logSuccess(final AuthenticationLogEntry authenticationLogEntry) {
            log.info("Authentication attempt success for: {}", authenticationLogEntry);
            successLog.computeIfPresent(authenticationLogEntry.getPrincipal(),(principal, l) -> l.append(authenticationLogEntry));
            successLog.computeIfAbsent(authenticationLogEntry.getPrincipal(),(principal) -> List.of(authenticationLogEntry));
            return authenticationLogEntry;
        }
    }
}