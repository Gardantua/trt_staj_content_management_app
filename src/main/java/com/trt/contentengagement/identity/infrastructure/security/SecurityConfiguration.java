package com.trt.contentengagement.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    UserDetailsService noStoredPasswordUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Local username and password authentication is not configured."
            );
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            SecurityErrorResponseWriter errorResponseWriter,
            ObjectProvider<TemporaryHeaderAuthenticationFilter> temporaryAuthenticationFilter
    ) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorResponseWriter.write(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "AUTHENTICATION_REQUIRED",
                                        "Authentication is required to access this resource."
                                ))
                        .accessDeniedHandler((request, response, exception) ->
                                errorResponseWriter.write(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED",
                                        "The authenticated actor does not have permission."
                                )));

        TemporaryHeaderAuthenticationFilter localAuthenticationFilter =
                temporaryAuthenticationFilter.getIfAvailable();
        if (localAuthenticationFilter != null) {
            httpSecurity.addFilterBefore(
                    localAuthenticationFilter,
                    AnonymousAuthenticationFilter.class
            );
        }

        return httpSecurity.build();
    }
}
