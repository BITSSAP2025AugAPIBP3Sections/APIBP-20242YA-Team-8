# JWT Authentication in Vaultify

## Table of Contents
1. [Overview](#overview)
2. [What is JWT?](#what-is-jwt)
3. [Architecture & Flow](#architecture--flow)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Security Features](#security-features)
7. [Code Walkthrough](#code-walkthrough)
8. [Best Practices Used](#best-practices-used)

---

## Overview

Vaultify uses **JSON Web Token (JWT)** authentication to provide secure, stateless authentication for its file management system. This implementation ensures that:

- ✅ Users can securely authenticate without maintaining server-side sessions
- ✅ API requests are stateless and scalable
- ✅ Token-based access control is enforced across all protected endpoints
- ✅ Frontend and backend are decoupled with token-based communication

---

## What is JWT?

**JSON Web Token (JWT)** is a compact, URL-safe token format that consists of three parts:

```
Header.Payload.Signature
```

1. **Header**: Contains token type and signing algorithm (HS256)
2. **Payload**: Contains claims (username, roles, expiration)
3. **Signature**: Ensures token integrity and authenticity

### JWT Structure Example
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VybmFtZSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDA4NjQwMH0.signature
```

---

## Architecture & Flow

### Authentication Flow Diagram

```
┌─────────┐                    ┌──────────┐                    ┌─────────┐
│ Client  │                    │ Backend  │                    │ Database│
└────┬────┘                    └────┬─────┘                    └────┬────┘
     │                               │                               │
     │  1. POST /auth/login          │                               │
     │  {username, password}         │                               │
     ├──────────────────────────────>│                               │
     │                               │  2. Validate credentials      │
     │                               ├──────────────────────────────>│
     │                               │<──────────────────────────────┤
     │                               │  3. Generate JWT              │
     │                               │  (JwtUtil.generateToken)      │
     │                               │                               │
     │  4. Return JWT token          │                               │
     │<──────────────────────────────┤                               │
     │                               │                               │
     │  5. Store token in            │                               │
     │     localStorage              │                               │
     │                               │                               │
     │  6. API Request + Bearer Token│                               │
     │  Authorization: Bearer <JWT>  │                               │
     ├──────────────────────────────>│                               │
     │                               │  7. Validate JWT              │
     │                               │  (JwtAuthFilter)              │
     │                               │                               │
     │                               │  8. Load UserDetails          │
     │                               ├──────────────────────────────>│
     │                               │<──────────────────────────────┤
     │                               │                               │
     │  9. Authenticated Response    │                               │
     │<──────────────────────────────┤                               │
```

### Step-by-Step Flow

1. **User Registration/Login**
   - Client sends credentials to `/auth/login` or `/auth/register`
   - Backend validates credentials using Spring Security's `AuthenticationManager`
   - On success, generates JWT token using `JwtUtil`

2. **Token Generation**
   - Token includes: username (subject), roles, issued time, expiration
   - Signed with HMAC-SHA256 using secret key
   - Default expiration: 24 hours (86,400,000 ms)

3. **Token Storage (Frontend)**
   - Token stored in browser's `localStorage`
   - Automatically attached to all API requests via Axios interceptor

4. **Request Authentication**
   - Every protected request includes: `Authorization: Bearer <token>`
   - `JwtAuthFilter` intercepts requests before reaching controllers
   - Validates token signature and expiration
   - Loads user details and sets Spring Security context

5. **Authorization**
   - Spring Security checks authentication status
   - Method-level security can enforce role-based access

---

## Backend Implementation

### 1. JwtUtil - Token Generation & Validation

**Location**: `backend/src/main/java/com/rip/vaultify/security/JwtUtil.java`

**Key Responsibilities**:
- Generate JWT tokens with user claims
- Validate token signature and expiration
- Extract username from tokens

**Key Features**:
```java
// Token Generation
public String generateToken(String username, List<String> roles) {
    long now = System.currentTimeMillis();
    JwtBuilder b = Jwts.builder()
            .setSubject(username)                    // User identifier
            .setIssuedAt(new Date(now))              // Token creation time
            .setExpiration(new Date(now + expirationMs))  // Expiration (24h default)
            .signWith(key, SignatureAlgorithm.HS256);     // HMAC-SHA256 signing
    
    if (roles != null && !roles.isEmpty()) {
        b.claim("roles", roles);                    // Role-based access control
    }
    return b.compact();
}

// Token Validation
public Jws<Claims> validateAndParse(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(key)                      // Verify signature
            .build()
            .parseClaimsJws(token);                  // Throws JwtException if invalid
}
```

**Configuration**:
- Secret key: Minimum 32 characters (from `application.properties`)
- Algorithm: HMAC-SHA256 (HS256)
- Expiration: Configurable (default: 86,400,000 ms = 24 hours)

---

### 2. JwtAuthFilter - Request Interceptor

**Location**: `backend/src/main/java/com/rip/vaultify/security/JwtAuthFilter.java`

**Key Responsibilities**:
- Intercept all HTTP requests
- Extract JWT from `Authorization` header
- Validate token and set Spring Security context
- Skip authentication for public endpoints

**Filter Order**: `@Order(2)` - Runs after rate limiting, before authentication

**Key Logic**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    // Skip JWT validation for public endpoints
    String path = request.getRequestURI();
    if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // Extract token from Authorization header
    final String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        
        // Validate token and extract username
        String username = jwtUtil.extractUsername(token);
        if (username != null) {
            // Load user details from database
            UserDetails ud = userDetailsService.loadUserByUsername(username);
            
            // Validate token signature and expiration
            jwtUtil.validateAndParse(token);
            
            // Set authentication in Spring Security context
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }
    
    filterChain.doFilter(request, response);
}
```

**Security Features**:
- Validates token signature to prevent tampering
- Checks expiration to prevent use of expired tokens
- Loads fresh user data from database (roles can be updated)
- Sets Spring Security context for authorization checks

---

### 3. SecurityConfig - Security Configuration

**Location**: `backend/src/main/java/com/rip/vaultify/config/SecurityConfig.java`

**Key Configuration**:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            // Disable CSRF (not needed for stateless JWT API)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Public endpoints (no authentication required)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register", ...)
                    .permitAll()
                .requestMatchers("/auth/me")
                    .authenticated()
                .anyRequest()
                    .authenticated())
            
            // Add JWT filter before authentication
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
```

**Key Points**:
- **Stateless**: No server-side sessions (JWT contains all auth info)
- **Filter Chain**: JWT filter runs before Spring Security authentication
- **Public Endpoints**: Login/register don't require authentication
- **Protected Endpoints**: All other endpoints require valid JWT

---

### 4. AuthController - Authentication Endpoints

**Location**: `backend/src/main/java/com/rip/vaultify/controller/AuthController.java`

**Endpoints**:

#### POST `/auth/register`
- Creates new user account
- Returns user ID and username (no token)

#### POST `/auth/login`
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
    // 1. Authenticate credentials
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(username, password));
    
    // 2. Extract user details and roles
    UserDetails ud = (UserDetails) auth.getPrincipal();
    List<String> roles = ud.getAuthorities().stream()
        .map(a -> a.getAuthority())
        .toList();
    
    // 3. Generate JWT token
    String token = jwtUtil.generateToken(username, roles);
    
    // 4. Return token and expiration
    return ResponseEntity.ok(Map.of(
        "token", token,
        "expiresInMs", 86400000
    ));
}
```

#### GET `/auth/me`
- Validates current JWT token
- Returns authenticated user information
- Used by frontend to verify token validity

---

### 5. CustomUserDetailsService - User Loading

**Location**: `backend/src/main/java/com/rip/vaultify/security/CustomUserDetailsService.java`

**Purpose**: Loads user details from database for Spring Security

```java
@Override
public UserDetails loadUserByUsername(String username) {
    User u = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Convert roles to Spring Security authorities
    String[] authorities = u.getRoles().stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .toArray(new String[0]);
    
    return User.builder()
        .username(u.getUsername())
        .password(u.getPassword())
        .authorities(authorities)
        .build();
}
```

---

## Frontend Implementation

### 1. AuthContext - Authentication State Management

**Location**: `frontend/src/context/AuthContext.jsx`

**Key Features**:
- Manages authentication state (user, token, loading)
- Persists token in `localStorage`
- Handles login, register, logout
- Verifies token on app load

**Token Storage**:
```javascript
const TOKEN_STORAGE_KEY = 'token';
const USER_STORAGE_KEY = 'vaultify_user';

// Store token after login
localStorage.setItem(TOKEN_STORAGE_KEY, token);

// Restore on app load
useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
        // Verify token with server
        fetchUser();
    }
}, []);
```

**Login Flow**:
```javascript
const login = async (username, password) => {
    // 1. Clear all caches
    queryClient.clear();
    
    // 2. Call login API
    const response = await authAPI.login(username, password);
    const { token } = response.data;
    
    // 3. Store token
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    
    // 4. Fetch user details
    await fetchUser();
    
    return response.data;
};
```

**Token Verification**:
```javascript
const fetchUser = async () => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) return;
    
    try {
        // Verify token with /auth/me endpoint
        const response = await authAPI.getMe();
        setUser(response.data);
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.data));
    } catch (error) {
        if (error.response?.status === 401) {
            // Token expired or invalid - clear storage
            localStorage.removeItem(TOKEN_STORAGE_KEY);
            setUser(null);
        }
    }
};
```

---

### 2. API Interceptor - Automatic Token Injection

**Location**: `frontend/src/services/api.js`

**Axios Request Interceptor**:
```javascript
// Automatically add token to all requests
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
```

**Axios Response Interceptor**:
```javascript
// Handle 401 Unauthorized (expired/invalid token)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Clear caches and redirect to login
            queryClient.clear();
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);
```

**Benefits**:
- ✅ Token automatically included in all API requests
- ✅ No need to manually add token to each request
- ✅ Automatic logout on token expiration
- ✅ Centralized error handling

---

## Security Features

### 1. Token Security

- **HMAC-SHA256 Signing**: Prevents token tampering
- **Secret Key**: Minimum 32 characters, stored in `application.properties`
- **Expiration**: 24-hour default (configurable)
- **Signature Validation**: Every request validates token signature

### 2. Password Security

- **BCrypt Hashing**: Passwords are hashed using BCrypt
- **No Plain Text Storage**: Passwords never stored in plain text
- **Secure Comparison**: Spring Security handles secure password comparison

### 3. Request Security

- **Stateless**: No server-side sessions (scalable)
- **CSRF Disabled**: Not needed for stateless JWT API
- **CORS Configured**: Only allows specific origins
- **Rate Limiting**: Separate limits for auth endpoints (5 req/min) and API (100 req/min)

### 4. Authorization

- **Role-Based Access Control (RBAC)**: Roles stored in JWT claims
- **Method Security**: `@EnableMethodSecurity` for method-level protection
- **User Context**: Spring Security context available in all controllers

---

## Code Walkthrough

### Complete Authentication Flow Example

#### 1. User Login Request
```http
POST /auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "securePassword123"
}
```

#### 2. Backend Processing
```java
// AuthController receives request
Authentication auth = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken("john_doe", "securePassword123"));

// JwtUtil generates token
String token = jwtUtil.generateToken("john_doe", ["ROLE_USER"]);
// Result: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 3. Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresInMs": 86400000
}
```

#### 4. Subsequent API Request
```http
GET /api/folders
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 5. JWT Filter Processing
```java
// JwtAuthFilter extracts token
String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

// Validates and extracts username
String username = jwtUtil.extractUsername(token); // "john_doe"

// Loads user details
UserDetails ud = userDetailsService.loadUserByUsername("john_doe");

// Sets authentication context
SecurityContextHolder.getContext().setAuthentication(auth);
```

#### 6. Controller Access
```java
@GetMapping("/api/folders")
public ResponseEntity<?> getFolders(Authentication authentication) {
    // Authentication is automatically available
    String username = authentication.getName(); // "john_doe"
    // ... fetch folders for user
}
```

---

## Best Practices Used

### ✅ Security Best Practices

1. **Strong Secret Key**: Minimum 32 characters, stored securely
2. **Token Expiration**: Prevents indefinite token usage
3. **Signature Validation**: Every request validates token integrity
4. **Password Hashing**: BCrypt with salt
5. **Stateless Design**: Scalable, no server-side sessions
6. **HTTPS Recommended**: Token should be transmitted over HTTPS in production

### ✅ Implementation Best Practices

1. **Separation of Concerns**:
   - `JwtUtil`: Token generation/validation
   - `JwtAuthFilter`: Request interception
   - `AuthController`: Authentication endpoints
   - `SecurityConfig`: Security configuration

2. **Error Handling**:
   - Graceful token expiration handling
   - Clear error messages
   - Automatic logout on 401

3. **Frontend Integration**:
   - Automatic token injection via interceptors
   - Token persistence in localStorage
   - Token verification on app load

4. **Configuration**:
   - Configurable expiration time
   - Environment-based secret key
   - Separate rate limits for auth endpoints

---

## Configuration

### Backend Configuration (`application.properties`)
```properties
# JWT Configuration
vaultify.jwt.secret=9c8099fc-e4c7-4246-bdfa-24e86e281305
vaultify.jwt.expirationMs=86400000  # 24 hours in milliseconds
```

### Frontend Configuration
- Token stored in `localStorage` with key `'token'`
- User data stored with key `'vaultify_user'`
- API base URL: `http://localhost:8080`

---

## Testing JWT Authentication

### 1. Register User
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}'
```

### 2. Login and Get Token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresInMs": 86400000
}
```

### 3. Use Token for Authenticated Request
```bash
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 4. Verify Token
```bash
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Summary

Vaultify implements a **robust, production-ready JWT authentication system** that:

- ✅ Provides stateless, scalable authentication
- ✅ Secures API endpoints with token-based access
- ✅ Integrates seamlessly with Spring Security
- ✅ Handles token expiration and refresh gracefully
- ✅ Follows security best practices
- ✅ Maintains clean separation of concerns

The implementation demonstrates a complete authentication flow from user registration/login through token generation, validation, and protected API access.

---

## Additional Resources

- [JWT.io](https://jwt.io/) - JWT debugger and documentation
- [Spring Security JWT Guide](https://spring.io/guides/topicals/spring-security-architecture)
- [JJWT Library Documentation](https://github.com/jwtk/jjwt)

---

**Presentation Date**: Generated for Vaultify Project  
**Version**: 1.0

