import os

base_pkg = "src/main/java/com/recallx/recallx"

os.makedirs(f"{base_pkg}/security", exist_ok=True)
os.makedirs(f"{base_pkg}/security/jwt", exist_ok=True)
os.makedirs(f"{base_pkg}/security/services", exist_ok=True)
os.makedirs(f"{base_pkg}/dto/request", exist_ok=True)
os.makedirs(f"{base_pkg}/dto/response", exist_ok=True)

files = {}

# Role Enum
files[f"{base_pkg}/entity/Role.java"] = """package com.recallx.recallx.entity;
public enum Role {
    ADMIN, ENGINEER, VIEWER
}
"""

# AuthController
files[f"{base_pkg}/controller/AuthController.java"] = """package com.recallx.recallx.controller;
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
"""

# Security Config (Basic Scaffold)
files[f"{base_pkg}/security/SecurityConfig.java"] = """package com.recallx.recallx.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/health").permitAll()
                    .anyRequest().authenticated()
            );
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
"""

# DTOs
files[f"{base_pkg}/dto/request/LoginRequest.java"] = """package com.recallx.recallx.dto.request;
import lombok.Data;
@Data public class LoginRequest { private String username; private String password; }
"""
files[f"{base_pkg}/dto/request/SignupRequest.java"] = """package com.recallx.recallx.dto.request;
import lombok.Data;
@Data public class SignupRequest { private String username; private String password; private Long organizationId; }
"""

# Test Scaffold
os.makedirs("src/test/java/com/recallx/recallx/security", exist_ok=True)
files["src/test/java/com/recallx/recallx/security/SecurityIntegrationTest.java"] = """package com.recallx.recallx.security;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
public class SecurityIntegrationTest {
    @Test
    public void contextLoads() {}
}
"""

for path, content in files.items():
    with open(path, "w") as f:
        f.write(content)

print("Scaffolded Security.")
