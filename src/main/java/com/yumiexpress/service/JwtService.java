package com.yumiexpress.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateToken(String phoneNumber, String role) {
        return Jwts.builder()
                .claim("role", role)
                .subject(phoneNumber) // JJWT 0.12+ uses .subject() instead of .setSubject()
                .issuedAt(new Date(System.currentTimeMillis())) // JJWT 0.12+ uses .issuedAt()
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(key, Jwts.SIG.HS256) // Force the algorithm
                .compact();
    }

    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}