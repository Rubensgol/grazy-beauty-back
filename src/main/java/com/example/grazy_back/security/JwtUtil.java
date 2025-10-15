package com.example.grazy_back.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

import java.security.Key;
import java.util.Date;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil 
{

    @Value("${app.jwt.secret:${JWT_SECRET}}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long expirationMs;

    private Key key() 
    {
        if (jwtSecret == null || jwtSecret.isBlank()) 
            throw new IllegalStateException("JWT secret is not configured. Set app.jwt.secret or JWT_SECRET.");

        byte[] keyBytes;

        try 
        {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } 
        catch (IllegalArgumentException e) 
        {
            keyBytes = jwtSecret.getBytes();
        }

        try 
        {
            return Keys.hmacShaKeyFor(keyBytes);
        }
         catch (WeakKeyException e) 
        {
            throw new IllegalStateException("JWT secret too weak. Provide a 256-bit (32+ bytes) secret. You can use a base64-encoded value.", e);
        }
    }

    public String generateToken(String username) 
    {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsername(String token) 
    {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validate(String token)
    {
        try 
        {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } 
        catch (Exception ex) 
        {
            return false;
        }
    }
}
