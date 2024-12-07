package tech.webclouds.simpledrivejava.configs;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tech.webclouds.simpledrivejava.helpers.TokenAgent;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenAgent tokenProvider;

    public SecurityConfig(TokenAgent tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Using BCrypt for password encoding
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity; customize as needed
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**").permitAll() // Allow public access to log in and index pages
                        .anyRequest().authenticated() // Protect all other endpoints
                )
                .oauth2ResourceServer(configurer -> configurer.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint((request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized: Authentication required")
                                )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Access is denied")
                        ));
        return http.build();
    }
}
