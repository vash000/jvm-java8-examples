package io.vash.example.spring.security.auth.basic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vash.example.spring.security.auth.basic.service.AuthenticationJournal;
import io.vash.example.spring.security.auth.basic.service.AuthenticationVault;
import io.vash.example.spring.security.auth.basic.service.AuthenticationVault.IdentifierMismatch;
import io.vavr.jackson.datatype.VavrModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.NullRequestCache;

import java.util.Objects;

import static io.vash.example.spring.security.auth.basic.AuthenticationApi.ENDPOINT_AUTH;
import static io.vash.example.spring.security.auth.basic.RegistrationApi.ENDPOINT_REGISTER;

/**
 * Basic Authentication Service
 *
 * export AUTH_ENDPOINT='http://localhost:8080'
 * export AUTH_CONTENT_JSON='Content-Type: application/json; charset=utf-8'
 * alias auth_service='curl -H "$AUTH_CONTENT_JSON" '
 *
 *
 * <pre>
 *     {@code
 *     # Register user
 *     > export AUTH_USER_LOGIN='[YOUR_NAME]'
 *     > auth_service -X "POST" "$AUTH_ENDPOINT/register" -d '{"login": "'$AUTH_USER_LOGIN'"}'
 *
 *     # Login with user data
 *     > export AUTH_USER_BASIC="-u $AUTH_USER_LOGIN:dafbd4a4-1b5d-4593-a22b-dacb6145b6ec"
 *	   > auth_service -X "POST" "$AUTH_ENDPOINT/auth" $AUTH_USER_BASIC -v 2>&1 | grep JSESSIONID
 *
 *
 * 	   # Access the user data
 * 	   > export AUTH_USER_TOKEN='Cookie: JSESSIONID=2DF606ABE16B3F7D88405836E5F1A06D'
 *
 *     auth_service -H "$AUTH_USER_TOKEN" "$AUTH_ENDPOINT/journal/auth/successful"
 *
 *     # Logout
 *     auth_service -H "$AUTH_USER_TOKEN" "$AUTH_ENDPOINT/logout"
 *     }
 * </pre>
 */
@Slf4j
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		log.info("Start ");
		SpringApplication.run(Application.class, args);
	}

	@Primary
	@Configuration
	class CustomObjectMapper extends ObjectMapper {
		public CustomObjectMapper() {
			registerModule(new VavrModule());
		}
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Configuration
	@EnableWebSecurity
	static class RestSecurityConfig extends WebSecurityConfigurerAdapter {

		private final AuthenticationProvider authenticationProvider;

		@Autowired
		RestSecurityConfig(AuthenticationProvider authenticationProvider) {
			this.authenticationProvider = Objects.requireNonNull(authenticationProvider);
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
				.antMatchers( ENDPOINT_REGISTER).permitAll()
				.anyRequest().authenticated()
				.and()
					.requestCache().requestCache(new NullRequestCache())
				.and()
					.httpBasic()
				.and()
					.logout().logoutSuccessUrl(ENDPOINT_AUTH)
				.and()
					.csrf().disable(); //Not needed
		}

		@Autowired
		void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(authenticationProvider);
		}
	}

	@Configuration
	static class BasicAuthenticationProvider implements AuthenticationProvider {

		private final AuthenticationVault vault;

		@Autowired
		BasicAuthenticationProvider(AuthenticationVault vault) {
			this.vault = vault;
		}

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			try {
				return vault.match(authentication);
			} catch (IdentifierMismatch e) {
				throw new AuthenticationCredentialsNotFoundException(e.getMessage());
			}
		}

		@Override
		public boolean supports(Class<?> authentication) {
			return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
		}
	}

	@Configuration
	@Slf4j
	static class BasicAuthenticationPub implements AuthenticationEventPublisher {

		final AuthenticationJournal journal;

		@Autowired
		BasicAuthenticationPub(AuthenticationJournal journal) {
			this.journal = journal;
		}

		@Override
		public void publishAuthenticationSuccess(Authentication authentication) {
			journal.success(authentication);
		}

		@Override
		public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
			log.info("publishAuthenticationFailure {}", authentication);
		}
	}

}
