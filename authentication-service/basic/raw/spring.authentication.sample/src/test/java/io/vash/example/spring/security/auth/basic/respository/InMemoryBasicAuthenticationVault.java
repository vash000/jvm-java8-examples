package io.vash.example.spring.security.auth.basic.respository;

import io.vash.example.spring.security.auth.basic.service.AuthenticationVault;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;


@Slf4j
class InMemoryBasicAuthenticationVault implements AuthenticationVault {

    final HashMap<Key,Object> vault = new HashMap<>();

    @Override
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

    private Option<Object> findSecret(final Key key) {
        log.info("Lookup for key: {}",key);
        return Option.of(this.vault.get(key))
                .onEmpty(() -> log.info("Not found key: {}", key));
    }

    private Option<Authentication> findMatch(final Authentication authentication) {
        final Key key = Key.of(authentication);
        return findSecret(key)
                .map( secret -> compare(key,secret, authentication));
    }

    private Authentication compare(Key key, Object secretCredentials, Authentication authentication) {
        log.info("Compare secret for matching key {}",key);

        final Object testCredentials = authentication.getCredentials();

        if(secretCredentials.equals(authentication.getCredentials())) {
            log.info("Provided credentials are valid.");
            return new UsernamePasswordAuthenticationToken(key.getName(),testCredentials, Collections.emptyList());
        } else {
            log.info("Provided credentials are invalid");
            return null;
        }
    }

    private Try<Authentication> tryToRegister(final Principal principal) {
        final Key key = Key.of(principal);
        log.info("Attempt to register in vault {}",key);
        return vault.containsKey(key) ? Try.failure(collision(principal)) : Try.success(putToken(key));
    }

    private Authentication putToken(final Key key) {
        log.info("Adding key to vault {}",key);
        final Authentication authentication = generateAuthetication(key);
        final Object removed = this.vault.put(key, authentication.getCredentials());
        if(removed == null) return authentication; else throw collision(key);
    }

    private Authentication generateAuthetication(Principal principal) {
        return new UsernamePasswordAuthenticationToken(principal.getName(),nextToken());
    }

    private String nextToken() {
        //this.vault.containsValue()
        //Create a unique token
        return UUID.randomUUID().toString();
    }

    @Value
    private static class Key implements Principal{

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