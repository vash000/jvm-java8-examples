package io.vash.example.spring.security.auth.basic;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
class AuthenticationApi {

    static final String ENDPOINT_AUTH = "/auth";

    @RequestMapping(ENDPOINT_AUTH)
    @ResponseStatus(HttpStatus.OK)
    void authenticate() {
        //magic
    }

}