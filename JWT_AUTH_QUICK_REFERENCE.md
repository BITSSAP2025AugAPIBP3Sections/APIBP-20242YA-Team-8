# JWT Authentication - Quick Reference Guide

## 🎯 Key Points for Presentation

### What is JWT Authentication?
- **Stateless**: No server-side sessions needed
- **Token-based**: Client stores token, sends with each request
- **Secure**: Signed with HMAC-SHA256 to prevent tampering
- **Scalable**: Works across multiple servers without shared session storage

---

## 📋 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    JWT Authentication Flow                   │
└─────────────────────────────────────────────────────────────┘

1. LOGIN
   Client → POST /auth/login → Backend validates → Returns JWT

2. STORAGE
   Frontend stores JWT in localStorage

3. REQUESTS
   Client → API Request + Bearer Token → JWT Filter validates → Controller

4. VALIDATION
   - Extract token from Authorization header
   - Verify signature
   - Check expiration
   - Load user details
   - Set Spring Security context
```

---

## 🔑 Core Components

### Backend Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **JwtUtil** | Generate & validate tokens | `security/JwtUtil.java` |
| **JwtAuthFilter** | Intercept requests, validate JWT | `security/JwtAuthFilter.java` |
| **SecurityConfig** | Configure security rules | `config/SecurityConfig.java` |
| **AuthController** | Login/register endpoints | `controller/AuthController.java` |
| **CustomUserDetailsService** | Load user from database | `security/CustomUserDetailsService.java` |

### Frontend Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **AuthContext** | Manage auth state & token | `context/AuthContext.jsx` |
| **API Interceptor** | Auto-inject token in requests | `services/api.js` |

---

## 🔐 Security Features

✅ **Token Security**
- HMAC-SHA256 signing
- 32+ character secret key
- 24-hour expiration (configurable)
- Signature validation on every request

✅ **Password Security**
- BCrypt hashing
- No plain text storage

✅ **Request Security**
- Stateless (no sessions)
- CORS configured
- Rate limiting (5 req/min for auth, 100 req/min for API)

---

## 📝 Token Structure

```
Header:  { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "username", "roles": ["ROLE_USER"], "iat": ..., "exp": ... }
Signature: HMAC-SHA256(header + payload + secret)
```

**Token Format**: `Header.Payload.Signature`

---

## 🔄 Complete Flow Example

### 1. User Logs In
```http
POST /auth/login
{ "username": "john", "password": "pass123" }
```

### 2. Backend Returns Token
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresInMs": 86400000
}
```

### 3. Frontend Stores Token
```javascript
localStorage.setItem('token', token);
```

### 4. Subsequent Requests Include Token
```http
GET /api/folders
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 5. Backend Validates & Processes
- JwtAuthFilter extracts token
- Validates signature & expiration
- Loads user details
- Sets authentication context
- Controller receives authenticated request

---

## 💻 Code Snippets

### Token Generation (Backend)
```java
String token = jwtUtil.generateToken(username, roles);
// Includes: username, roles, issued time, expiration
// Signed with: HMAC-SHA256
```

### Token Validation (Backend)
```java
Jws<Claims> claims = jwtUtil.validateAndParse(token);
// Throws exception if invalid/expired
```

### Token Injection (Frontend)
```javascript
// Automatic via Axios interceptor
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
```

---

## 🎨 Presentation Tips

### Slide 1: Introduction
- "JWT Authentication in Vaultify"
- Stateless, secure, scalable

### Slide 2: What is JWT?
- Three-part structure (Header.Payload.Signature)
- Self-contained token
- No server-side storage needed

### Slide 3: Architecture Flow
- Show the flow diagram
- Highlight stateless nature

### Slide 4: Backend Implementation
- JwtUtil: Token generation/validation
- JwtAuthFilter: Request interception
- SecurityConfig: Security rules

### Slide 5: Frontend Implementation
- AuthContext: State management
- API Interceptor: Automatic token injection
- localStorage: Token persistence

### Slide 6: Security Features
- Token signing & validation
- Password hashing
- Rate limiting
- CORS configuration

### Slide 7: Demo
- Show login flow
- Show token in browser DevTools
- Show authenticated API request

---

## 🧪 Testing Commands

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# 2. Login (get token)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# 3. Use token
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer <TOKEN>"

# 4. Verify token
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📊 Key Metrics

- **Token Expiration**: 24 hours (86,400,000 ms)
- **Secret Key Length**: Minimum 32 characters
- **Algorithm**: HMAC-SHA256 (HS256)
- **Rate Limits**: 
  - Auth endpoints: 5 requests/minute
  - API endpoints: 100 requests/minute

---

## ✅ Best Practices Implemented

1. ✅ Strong secret key (32+ chars)
2. ✅ Token expiration
3. ✅ Signature validation
4. ✅ Password hashing (BCrypt)
5. ✅ Stateless design
6. ✅ Automatic token injection
7. ✅ Graceful error handling
8. ✅ Token verification on app load

---

## 🚀 Production Considerations

- Use environment variables for secret key
- Enable HTTPS (tokens in headers)
- Consider refresh tokens for longer sessions
- Implement token blacklisting for logout
- Monitor token expiration patterns
- Set appropriate CORS origins

---

**Quick Reference for JWT Authentication Presentation**

