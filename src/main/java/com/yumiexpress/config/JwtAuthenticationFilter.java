package com.yumiexpress.config;

import com.yumiexpress.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get the Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phoneNumber;

        // 2. If there is no header, or it doesn't start with "Bearer ", pass to the next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the token (remove the word "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // 4. Extract the phone number from the token
            phoneNumber = jwtService.extractPhoneNumber(jwt);

            // 5. If we found a phone number and the user isn't already authenticated:
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // In a full production app, we would load the user from the database here.
                // For the MVP, if the token is valid, we authenticate them.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        phoneNumber,
                        null,
                        Collections.emptyList()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Set the authentication in the context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token is invalid or expired
            System.out.println("Invalid JWT token: " + e.getMessage());
        }

        // 7. Continue the request
        filterChain.doFilter(request, response);
    }
}