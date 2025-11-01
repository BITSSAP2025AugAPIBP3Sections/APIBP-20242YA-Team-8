package com.rip.vaultify.controller;

import com.rip.vaultify.model.User;
import com.rip.vaultify.security.JwtUtil;
import com.rip.vaultify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // DTOs are simple maps for brevity
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }

        try {
            User user = userService.register(username, password);
            return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            UserDetails ud = (UserDetails) auth.getPrincipal();

            List<String> roles = ud.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();

            String token = jwtUtil.generateToken(username, roles);
            return ResponseEntity.ok(Map.of("token", token, "expiresInMs", 86400000));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "authentication failed"));
        }
    }

    // Example endpoint to verify token and return user context
    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }
}
