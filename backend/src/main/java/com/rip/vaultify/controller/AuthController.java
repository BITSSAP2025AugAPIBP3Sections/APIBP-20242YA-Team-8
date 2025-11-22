package com.rip.vaultify.controller;

import com.rip.vaultify.config.LoggingConfig;
import com.rip.vaultify.model.User;
import com.rip.vaultify.security.JwtUtil;
import com.rip.vaultify.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");
    private static final Marker AUTH_SUCCESS_MARKER = MarkerFactory.getMarker("AUTH_SUCCESS");
    private static final Marker AUTH_FAILURE_MARKER = MarkerFactory.getMarker("AUTH_FAILURE");

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

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided username and password. The username must be unique."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"id\": 1, \"username\": \"john_doe\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - username or password missing",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"username and password required\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - username already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Username already exists\"}")
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration credentials",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"username\": \"john_doe\", \"password\": \"securePassword123\"}")
                    )
            )
            @RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");

        logger.info(SECURITY_MARKER, "User registration attempt for username: {}", username);
        LoggingConfig.LoggingContext.addContext("action", "register");
        LoggingConfig.LoggingContext.addContext("targetUsername", username);

        if (username == null || password == null) {
            logger.warn(SECURITY_MARKER, "Registration failed - missing credentials for username: {}", username);
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }

        try {
            User user = userService.register(username, password);
            LoggingConfig.LoggingContext.setUserId(user.getId());
            
            logger.info(AUTH_SUCCESS_MARKER, "User registration successful for username: {}, userId: {}", 
                       username, user.getId());
            
            return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
        } catch (IllegalArgumentException ex) {
            logger.warn(AUTH_FAILURE_MARKER, "Registration failed for username: {} - {}", username, ex.getMessage());
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("targetUsername");
        }
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates a user and returns a JWT token. Use this token in the Authorization header for protected endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"expiresInMs\": 86400000}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - username or password missing",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"username and password required\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"invalid credentials\"}")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"username\": \"john_doe\", \"password\": \"securePassword123\"}")
                    )
            )
            @RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        
        logger.info(SECURITY_MARKER, "Login attempt for username: {}", username);
        LoggingConfig.LoggingContext.addContext("action", "login");
        LoggingConfig.LoggingContext.addContext("targetUsername", username);
        
        if (username == null || password == null) {
            logger.warn(SECURITY_MARKER, "Login failed - missing credentials for username: {}", username);
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
            
            // Set user context for logging
            LoggingConfig.LoggingContext.setUserId(username);
            LoggingConfig.LoggingContext.addContext("roles", String.join(",", roles));
            
            logger.info(AUTH_SUCCESS_MARKER, "Login successful for username: {}, roles: {}", 
                       username, roles);
            
            return ResponseEntity.ok(Map.of("token", token, "expiresInMs", 86400000));
        } catch (BadCredentialsException ex) {
            logger.warn(AUTH_FAILURE_MARKER, "Login failed - invalid credentials for username: {}", username);
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        } catch (DisabledException ex) {
            logger.warn(AUTH_FAILURE_MARKER, "Login failed - account disabled for username: {}", username);
            return ResponseEntity.status(401).body(Map.of("error", "account disabled"));
        } catch (LockedException ex) {
            logger.warn(AUTH_FAILURE_MARKER, "Login failed - account locked for username: {}", username);
            return ResponseEntity.status(401).body(Map.of("error", "account locked"));
        } catch (Exception ex) {
            logger.error(AUTH_FAILURE_MARKER, "Login failed - unexpected error for username: {}", username, ex);
            return ResponseEntity.status(500).body(Map.of("error", "authentication failed"));
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("targetUsername");
            LoggingConfig.LoggingContext.removeContext("roles");
        }
    }

    @Operation(
            summary = "Get current user information",
            description = "Returns information about the currently authenticated user. Requires a valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"username\": \"john_doe\", \"authorities\": [\"ROLE_USER\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"unauthenticated\"}")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        logger.debug("Token verification request received");
        LoggingConfig.LoggingContext.addContext("action", "token_verification");
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn(SECURITY_MARKER, "Token verification failed - unauthenticated request");
                return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
            }
            
            String username = authentication.getName();
            LoggingConfig.LoggingContext.setUserId(username);
            
            logger.info("Token verification successful for user: {}", username);
            
            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "authorities", authentication.getAuthorities()
            ));
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
        }
    }
}
