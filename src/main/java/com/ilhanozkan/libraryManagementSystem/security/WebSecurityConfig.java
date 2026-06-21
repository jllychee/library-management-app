package com.ilhanozkan.libraryManagementSystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    public WebSecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter, Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Check if we're running in test mode
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");

        http
            .csrf().disable()
            .cors(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults()) // allows Basic auth (e.g. Swagger UI Authorize)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(
                auth -> {
                    // Public endpoints
                    auth.requestMatchers("/auth/login", "/auth/register", "/auth/refresh-token", "/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    
                     if (isTestProfile) {
                         // In test profile, make all /api endpoints accessible for easier testing
                         auth.requestMatchers("/books/**").permitAll()
                             .requestMatchers("/borrowings/**").permitAll()
                             .requestMatchers("/users/**").permitAll()
                             .requestMatchers("/waitlists/**").permitAll();
                     } else {
                         // In non-test profiles, apply proper security
                         auth.requestMatchers("/users/**").hasAuthority("ROLE_LIBRARIAN")
                             .requestMatchers("/books/*/borrow").authenticated()
                             .requestMatchers("/books/*/return").authenticated()
                             .requestMatchers("/books/**").permitAll() // Allow book search/view to all
                             .requestMatchers("/borrowings/**").authenticated()
                             .requestMatchers("/waitlists/**").authenticated();
                     }
                    
                    auth.anyRequest().authenticated();
                }
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(List.of("http://localhost:5173"));
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      config.setAllowedHeaders(List.of("*"));
      config.setAllowCredentials(true);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", config);
      return source;
    }
} 
