package com.trt.contentengagement.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    UserDetailsService noFormLoginUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Form login is not configured.");
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            SecurityErrorResponseWriter errorResponseWriter,
            ObjectProvider<TemporaryHeaderAuthenticationFilter> temporaryAuthenticationFilter,
            SecurityContextRepository securityContextRepository,
            @Value("${app.security.csrf-enabled:true}") boolean csrfEnabled
    ) throws Exception {
        TemporaryHeaderAuthenticationFilter localAuthenticationFilter =
                temporaryAuthenticationFilter.getIfAvailable();
        if (csrfEnabled) {
            httpSecurity.csrf(csrf -> {
                csrf.spa();
                if (localAuthenticationFilter != null) {
                    csrf.ignoringRequestMatchers(request -> request.getHeader(
                            TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER
                    ) != null);
                }
            });
        } else {
            httpSecurity.csrf(csrf -> csrf.disable());
        }

        httpSecurity
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/password-reset/request",
                                "/api/v1/auth/password-reset/reset"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorResponseWriter.write(
                                        request,
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "AUTHENTICATION_REQUIRED",
                                        "Authentication is required to access this resource."
                                ))
                        .accessDeniedHandler((request, response, exception) ->
                                errorResponseWriter.write(
                                        request,
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED",
                                        "The authenticated actor does not have permission."
                                )));

        if (localAuthenticationFilter != null) {
            httpSecurity.addFilterBefore(
                    localAuthenticationFilter,
                    AnonymousAuthenticationFilter.class
            );
        }

        return httpSecurity.build();
    }
}
