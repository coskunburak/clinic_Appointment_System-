package com.example.clinic.common.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Production-grade Spring Security configuration fulfilling WP-S04-03, WP-S04-04, NFR-001, and NFR-002.
 * <p>
 * Key security properties:
 * <ul>
 *   <li>BCrypt password encoder configured with cost factor 12.</li>
 *   <li>Explicit role-based route segmentation (/patient/**, /staff/**, /admin/**).</li>
 *   <li>Session fixation protection (changeSessionId) and session invalidation on logout.</li>
 *   <li>Strict CSRF protection enabled on all mutable HTTP requests (POST, PUT, DELETE).</li>
 *   <li>Deterministic role-based post-login redirection.</li>
 *   <li>Method security enabled for granular service-level enforcement.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(ClinicAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers(
                    "/",
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/vendor/**",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                .requestMatchers("/patient/**").hasRole("PATIENT")
                .requestMatchers("/staff/**").hasRole("STAFF")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (!response.isCommitted()) {
                        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        request.getRequestDispatcher("/error").forward(request, response);
                    }
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId())
            );

        return http.build();
    }
}
