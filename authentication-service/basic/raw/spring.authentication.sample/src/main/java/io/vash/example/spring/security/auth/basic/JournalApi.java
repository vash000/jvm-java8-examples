package io.vash.example.spring.security.auth.basic;

import io.vash.example.spring.security.auth.basic.service.AuthenticationInfo;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal.AuthenticationLogEntry;
import io.vavr.collection.List;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
 class JournalApi {

    private final AuthenticationInfo info;

    @Autowired
    JournalApi(AuthenticationInfo info) {
        this.info = info;
    }

    @RequestMapping("/journal/auth/successful")
    SuccessfulAuthenticationAttemptsResponse successfulAuthenticationAttempts(
            @AuthenticationPrincipal Principal user,
            @RequestParam(value="limit", defaultValue="5") int limit){

        return SuccessfulAuthenticationAttemptsResponse.of(info.successfulAttempts(user,limit)
                .map(AuthenticationLogEntry::getUtc)
                .toList());
    }

    @Value(staticConstructor = "of")
    private static class SuccessfulAuthenticationAttemptsResponse {
        List<Long> timestamps;
    }
}