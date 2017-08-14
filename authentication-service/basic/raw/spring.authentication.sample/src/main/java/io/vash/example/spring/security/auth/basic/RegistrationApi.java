package io.vash.example.spring.security.auth.basic;

import io.vash.example.spring.security.auth.basic.service.AuthenticationVault;
import io.vash.example.spring.security.auth.basic.service.AuthenticationVault.IdentifierConflict;
import io.vash.example.spring.security.auth.basic.service.AuthenticationVault.IdentifierMismatch;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.Objects;


@Slf4j
@RestController
class RegistrationApi {

    final static String ENDPOINT_REGISTER = "/register";

    private final Registrant registrant;

    @Autowired
    RegistrationApi(Registrant registrant) {
        this.registrant = registrant;
    }

    @RequestMapping("/register")
    RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) throws SecurityException {
        return tryToValidate(request)
                 .flatMap(this::tryToRegister)
                 .map(this::toSuccessfulRegistrationResponse)
                 .get();
    }

    private Try<ValidRegistrationRequest> tryToValidate(RegistrationRequest request) {
        log.debug("Requested to register {}", request);
        return ValidRegistrationRequest.tryFor(request.getLogin());
    }

    private Try<Authentication> tryToRegister(final ValidRegistrationRequest request) {
        log.debug("Attempt to register with name {}", request.getName());
        return this.registrant.register(request);
    }

    private RegistrationResponse toSuccessfulRegistrationResponse(final Authentication authentication) {
        log.debug("Registration succeeded for name {}",authentication.getPrincipal());
        return new RegistrationResponse(authentication.getPrincipal(),authentication.getCredentials());
    }

    @Service
    static class Registrant {

        private final AuthenticationVault vault;

        @Autowired
        Registrant(AuthenticationVault vault) {
            this.vault = vault;
        }

        Try<Authentication> register(final Principal principal) {
            Objects.requireNonNull(principal,"Identifier can not be null");
            try {
                return Try.success(vault.register(principal));
            } catch (SecurityException e) {
                return Try.failure(e);
            }
        }

    }

    @Data
    @NoArgsConstructor
    static class RegistrationRequest {
        @NotNull String login;
    }

    @Value(staticConstructor = "tryFor")
    static class ValidRegistrationRequest implements Principal {

        String name;

        private ValidRegistrationRequest(String name) {
            this.name = name;
        }



        static Try<ValidRegistrationRequest> tryFor(final String input) {
            return Validator.DEFAULT
                    .validate(input)
                    .fold(Try::failure,Try::success);
        }

        @Override
        public String getName() {
            return name;
        }

        enum Validator {
            DEFAULT(5);

            final int maxLength;

            Validator(int maxLength) {
                this.maxLength = maxLength;
            }

            Validation<IllegalArgumentException,ValidRegistrationRequest> validate(final String login) {
                return Objects.isNull(login)   ? Validation.invalid(new IllegalArgumentException("login must be provided")) :
                        login.trim().isEmpty()  ? Validation.invalid(new IllegalArgumentException("login can not be empty")) :
                        login.length() > maxLength ? Validation.invalid(new IllegalArgumentException("login to long, max length " + 5)) :
                        login.indexOf(' ') >= 0 ? Validation.invalid(new IllegalArgumentException("login can not be separated by space")) :
                        Validation.valid(new ValidRegistrationRequest(login)); //This should be externalized and
            }
        }
    }

    @Value
    static class RegistrationResponse {
        Object login;
        Object token;
    }


    @Slf4j
    @RestControllerAdvice
    static class RegistrationExceptionHandler extends ResponseEntityExceptionHandler {

        @ExceptionHandler({IdentifierConflict.class})
        @ResponseStatus(HttpStatus.CONFLICT)
        @ResponseBody
        ApiError handleIdentifierCollision(IdentifierConflict e) {
            log.debug("Identifier Conflict {}",e.getIdentifier());
            log.trace(e.getMessage(),e);
            return ApiError.error(HttpStatus.CONFLICT,e.getLocalizedMessage(),e.getMessage());
        }

        @ExceptionHandler({IdentifierMismatch.class})
        @ResponseStatus(HttpStatus.FORBIDDEN)
        @ResponseBody
        ApiError handleIdentifierCollision(IdentifierMismatch e) {
            log.debug("Identifier Mismatch {}",e.getPrincipal());
            log.trace(e.getMessage(),e);
            return ApiError.error(HttpStatus.FORBIDDEN,e.getLocalizedMessage(),e.getMessage());
        }

        @ExceptionHandler({IllegalArgumentException.class})
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ResponseBody
        ApiError handleIdentifierCollision(IllegalArgumentException e) {
            log.debug("Invalid data");
            log.trace(e.getMessage(),e);
            return ApiError.error(HttpStatus.BAD_REQUEST,"Invalid input",e.getMessage());
        }

        @Value
        static class ApiError {

            static ApiError error(HttpStatus status, String message, String error) {
                return new ApiError(status,message, error);
            }

            long timestamp = System.currentTimeMillis();
            HttpStatus status;
            String error;
            String message;
        }
    }
}
