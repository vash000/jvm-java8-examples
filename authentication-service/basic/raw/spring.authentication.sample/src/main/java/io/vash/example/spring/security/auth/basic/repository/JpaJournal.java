package io.vash.example.spring.security.auth.basic.repository;

import io.vash.example.spring.security.auth.basic.repository.JpaJournal.JpaAuthenticationData;
import io.vash.example.spring.security.auth.basic.service.AuthenticationInfo;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal.AuthenticationLogEntry;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal.AuthenticationState;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.security.Principal;
import java.util.Objects;


interface JpaJournal extends CrudRepository<JpaAuthenticationData,Long> {

    java.util.List<JpaAuthenticationData> findByKeyOrderByUtcDesc(String key, Pageable pageable);

    @Entity(name = "SuccessAuthLog")
    @AllArgsConstructor
    @NoArgsConstructor
    class JpaAuthenticationData {

        JpaAuthenticationData(AuthenticationLogEntry entry) {
            this.key = entry.getPrincipal().getName();
            this.utc = entry.getUtc();
        }

        @Id  @GeneratedValue(strategy= GenerationType.IDENTITY)
        long seq;
        String key;
        long utc;

    }

    @Slf4j
    @Service
    class SuccessAuthenticationJournalReader implements AuthenticationInfo {

        private final JpaJournal journal;

        @Autowired
        public SuccessAuthenticationJournalReader(JpaJournal journal) {
            this.journal = journal;
        }

        @Override
        public List<AuthenticationLogEntry> successfulAttempts(final Principal principal, int limit) {
            return Array.ofAll(load(principal,limit))
                    .map( data -> data.utc)
                    .map( utc -> new AuthenticationLogEntry(principal, AuthenticationState.SUCCESSFUL,utc))
                    .toList();
        }

        private Iterable<JpaAuthenticationData> load(Principal principal,int limit) {
            return journal.findByKeyOrderByUtcDesc(principal.getName(),new PageRequest(0,limit));
        }

        private AuthenticationLogEntry map(Principal principal, long utc) {
            //We valid before return
            return new AuthenticationLogEntry(principal,AuthenticationState.SUCCESSFUL,utc);
        }

    }

    @Slf4j
    @Service
    class SuccessAuthenticationJournalWriter implements AuthenticationJournal {

        private final JpaJournal journal;

        @Autowired
        public SuccessAuthenticationJournalWriter(JpaJournal journal) {
            this.journal = journal;
        }

        private long timestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public AuthenticationLogEntry success(final Principal principal) {
            Objects.requireNonNull(principal,"identifier can not be null");
            return logSuccess(new AuthenticationLogEntry(principal,AuthenticationState.SUCCESSFUL,timestamp()));
        }

        private AuthenticationLogEntry logSuccess(final AuthenticationLogEntry authenticationLogEntry) {
            log.info("Authentication attempt success for: {}", authenticationLogEntry);
            journal.save(new JpaAuthenticationData(authenticationLogEntry));
            return authenticationLogEntry;
        }

        private AuthenticationLogEntry logFailure(final AuthenticationLogEntry authenticationLogEntry) {
            log.info("Authentication attempt failed for: {}", authenticationLogEntry);
            return authenticationLogEntry;
        }

    }
}
