package io.vash.example.spring.security.auth.basic.repository;

import io.vash.example.spring.security.auth.basic.repository.JpaVault.CredentialsData;
import io.vash.example.spring.security.auth.basic.service.AuthenticationVault;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

interface JpaVault extends CrudRepository<CredentialsData,String> {

    @Data
    @Entity
    @Table(name = "credentials")
    @AllArgsConstructor
    @NoArgsConstructor
    class CredentialsData {
        @Id
        String key;
        String token;
    }

    @Slf4j
    @Primary
    @Repository
    class JpaBasicAuthenticationVault implements AuthenticationVault {

        private final JpaVault vault;
        private final PasswordEncoder encoder;

        @Autowired
        JpaBasicAuthenticationVault(JpaVault vault, PasswordEncoder encoder) {
            this.vault = vault;
            this.encoder = encoder;
        }

        @Override
        @Transactional
        public Authentication register(final Principal principal) throws SecurityException {
            Objects.requireNonNull(principal, "principal can not be null");
            return tryToRegister(principal).getOrElseThrow(() -> collision(principal));
        }

        @Override
        public Authentication match(final Authentication authentication) throws SecurityException {
            Objects.requireNonNull(authentication,"vault can not be null");
            return findMatch(authentication)
                    .getOrElseThrow( () -> mismatch(authentication));
        }

        private Option<CredentialsData> findSecret(final Key key) {
            log.info("Lookup for key: {}",key);
            return Option.of(this.vault.findOne(key.getValue()))
                    .onEmpty(() -> log.info("Not found key: {}", key));
        }

        private Option<Authentication> findMatch(final Authentication authentication) {
            final Key key = Key.of(authentication);
            return findSecret(key)
                    .map( secret -> compare(key, secret, authentication));
        }

        private Authentication compare(Key key, CredentialsData secretCredentials, Authentication authentication) {
            log.info("Compare secret for matching key {}",key);

            final Object testCredentials = authentication.getCredentials();

            if(testCredentials instanceof String && this.encoder.matches(testCredentials.toString(),secretCredentials.token)) {
                log.info("Provided vault are valid.");
                return new UsernamePasswordAuthenticationToken(key.getValue(),testCredentials, Collections.emptyList());
            } else {
                log.info("Provided vault are invalid");
                return null;
            }
        }

        private Try<Authentication> tryToRegister(final Principal principal) {
            final Key key = Key.of(principal);
            log.info("Attempt to register in vault {}",key);
            return vault.exists(key.getValue()) ? Try.failure(collision(principal)) : Try.success(putToken(key));
        }

        private Authentication putToken(final Principal principal) {
            log.info("Adding key to vault {}",principal);
            final String token = nextToken();
            final String secured = this.encoder.encode(token);
            this.vault.save(new CredentialsData(principal.getName(),secured));
            return new UsernamePasswordAuthenticationToken(principal.getName(),token);
        }

        private String nextToken() {
            return UUID.randomUUID().toString();
        }

        @Value
        private static class Key implements Principal  {

            String value;

            static Key of(Principal identifier) {
                return new Key(Objects.requireNonNull(identifier).getName());
            }

            private Key(String value) {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public String getName() {
                return value;
            }
        }
    }



}
