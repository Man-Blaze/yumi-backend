package com.yumiexpress.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Allow ALL Web Pages and Form Submissions
                        .requestMatchers("/", "/register", "/login", "/process-register", "/process-login", "/book", "/submit-booking", "/success", "/receipt/**").permitAll()
                        .requestMatchers("/customer-register", "/send-otp", "/verify-otp", "/customer-dashboard/**", "/rate-order/**").permitAll()

                        // 2. Allow Rider & Admin Portals AND form submissions
                        .requestMatchers("/rider-portal/**", "/rider/**").permitAll()
                        .requestMatchers("/admin-portal", "/admin/assign-rider/**").permitAll()

                        // 3. Allow API Endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/orders/book").permitAll()
                        .requestMatchers("/api/orders/confirm-payment").permitAll()
                        .requestMatchers("/api/orders/*/qrcode").permitAll()
                        .requestMatchers("/receiver/**").permitAll()
                        .requestMatchers("/seller-login", "/seller-dashboard").permitAll()
                        .requestMatchers("/warehouse/**").permitAll()
                        // Everything else requires a token
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}