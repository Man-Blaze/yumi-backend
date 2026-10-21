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
                        // 1. THE MOST SPECIFIC RULES FIRST
                        .requestMatchers("/admin/riders").permitAll()
                        .requestMatchers("/admin/customers").permitAll()
                        .requestMatchers("/admin/scan-seller-barcode/**").permitAll()
                        .requestMatchers("/admin/assign-rider/**").permitAll()
                        .requestMatchers("/admin-portal").permitAll()
                                .requestMatchers("/admin/edit-order/**").permitAll()     // <--- ADD THIS
                                .requestMatchers("/admin/cancel-order/**").permitAll()   // <--- ADD THIS
                        .requestMatchers("/warehouse/**").permitAll()

                                .requestMatchers("/rider-portal/**", "/rider/**").permitAll()
                                .requestMatchers("/rider/weigh-parcel/**").permitAll()

                        .requestMatchers("/customer-dashboard/**", "/rate-order/**").permitAll()
                        .requestMatchers("/customer-register", "/send-otp", "/verify-otp").permitAll()

                        .requestMatchers("/seller-login", "/seller-dashboard").permitAll()
                        .requestMatchers("/receiver/**").permitAll()
                        .requestMatchers("/receipt/**").permitAll()

                        // 2. PUBLIC PAGES
                        .requestMatchers("/", "/register", "/login", "/process-register", "/process-login").permitAll()
                        .requestMatchers("/book", "/submit-booking", "/success").permitAll()

                        // 3. APIS
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/orders/book").permitAll()
                        .requestMatchers("/api/orders/confirm-payment").permitAll()
                        .requestMatchers("/api/orders/*/qrcode").permitAll()

                        .requestMatchers("/finance-dashboard").permitAll()

                        // 4. CATCH-ALL (Must be last)
                       // .anyRequest().authenticated()
                        .anyRequest().permitAll()
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