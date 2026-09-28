package com.recallx.recallx.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser() {
        return ResponseEntity.ok(Map.of("message", "User authenticated"));
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser() {
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }
}
