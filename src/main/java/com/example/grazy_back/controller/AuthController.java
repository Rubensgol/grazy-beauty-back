package com.example.grazy_back.controller;

import com.example.grazy_back.dto.LoginRequest;
import com.example.grazy_back.dto.LoginResponse;
import com.example.grazy_back.security.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController 
{
    @Value("${app.admin.username:admin}")
    private String adminUser;

    @Value("${app.admin.password:admin}")
    private String adminPass;

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) 
    {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) 
    {
        if (req.getUsername() == null || req.getSenha() == null)
            return ResponseEntity.badRequest().build();
        
        if (req.getUsername().equals(adminUser) && req.getSenha().equals(adminPass)) 
        {
            String token = jwtUtil.generateToken(adminUser);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(401).build();
    }
}
