# Vaultify

A modern, secure file storage and sharing application built with Spring Boot (backend) and React (frontend). Vaultify provides Google Drive-like functionality with hierarchical folder organization, file sharing, offline access, and comprehensive API support.

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![React](https://img.shields.io/badge/React-19.1.1-blue)
![License](https://img.shields.io/badge/license-Unlicense-lightgrey)

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [API Documentation](#-api-documentation)
- [Rate Limiting](#-rate-limiting)
- [Security Features](#-security-features)
- [Architecture](#-architecture)
- [Development](#-development)
- [Testing](#-testing)
- [Documentation](#-documentation)

---

## ✨ Features

### ✅ Core Features

#### Authentication & Authorization
- **JWT-based Authentication**: Secure token-based authentication
- **User Registration**: Create new user accounts
- **User Login**: Secure login with password hashing (BCrypt)
- **Role-based Access Control**: User roles (ROLE_USER)
- **Session Management**: Stateless JWT tokens (24-hour expiration)

#### Folder Management
- **Hierarchical Structure**: Unlimited nesting levels (tree structure)
- **Root Folders**: Top-level folders without parent
- **Subfolders**: Nested folders with parent-child relationships
- **CRUD Operations**: Create, Read, Update, Delete folders
- **Folder Renaming**: Update folder names
- **Cascading Deletes**: Deleting folder removes all children and files
- **User Isolation**: Each user sees only their own folders
- **ETag Caching**: HTTP caching for performance optimization

#### File Management
- **File Upload**: Upload files to folders (max 10MB per file)
- **File Download**: Download files with proper content types
- **File Preview**: Preview files in browser
- **File Metadata**: Track file size, type, upload date
- **File Organization**: Files organized within folders
- **File Deletion**: Delete files with proper cleanup
- **File Copy**: Copy files to different folders
- **Idempotency Support**: Prevent duplicate uploads with idempotency keys

#### File Sharing & Permissions
- **Share Files**: Share files with other users
- **Permission Levels**: READ, WRITE, OWNER access levels
- **Permission Management**: View, update, and revoke permissions
- **Shared Files View**: View files shared with you
- **Accepted Files**: View files you've accepted access to
- **Permission Notifications**: Track unviewed shared files
- **Mark as Viewed**: Track which shared files have been viewed

#### Offline Functionality
- **Offline File Storage**: Store files locally using IndexedDB
- **Offline Access**: View and download files without internet
- **Offline Files Page**: Dedicated page for offline files
- **Automatic Caching**: Files cached when downloaded
- **Offline Indicator**: UI shows when offline

#### API Support
- **REST API**: Complete RESTful API with standard HTTP methods
- **GraphQL API**: Flexible GraphQL queries and mutations
- **Dual API Support**: Use either REST or GraphQL based on needs
- **API Documentation**: Interactive Swagger UI and GraphiQL

### 🔒 Security Features

#### Rate Limiting
- **Token Bucket Algorithm**: Industry-standard rate limiting (Bucket4j)
- **Three-Tier Limits**:
  - Authentication: 5 requests per 60 seconds (login + register shared)
  - General API: 100 requests per 60 seconds
  - File Upload: 30 requests per 60 seconds
- **Client Identification**: Per-user (authenticated) or per-IP (anonymous)
- **Rate Limit Headers**: X-RateLimit-* headers in responses
- **Configurable Limits**: Easy to adjust in `application.properties`

#### Security Measures
- **JWT Authentication**: Secure token-based authentication
- **Password Hashing**: BCrypt one-way hashing
- **CORS Protection**: Configured for specific origins
- **CSRF Protection**: Disabled for stateless API (appropriate for JWT)
- **Input Validation**: Request validation at controller and service layers
- **Authorization Checks**: User ownership validation for all operations

### 🚀 Performance Features

- **ETag Caching**: HTTP caching with ETags for folders and files
- **Lazy Loading**: JPA lazy loading for database relationships
- **React Query Caching**: Frontend API response caching
- **Optimistic Updates**: UI updates immediately, syncs with server
- **In-Memory Rate Limiting**: Fast Caffeine cache for rate limit buckets

---

## 🛠 Tech Stack

### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Security**: Spring Security with JWT (JJWT 0.11.5)
- **Rate Limiting**: Bucket4j 8.10.1 + Caffeine 3.1.8
- **Database**: H2 (development) / PostgreSQL (production ready)
- **ORM**: Spring Data JPA / Hibernate
- **API Documentation**: Springdoc OpenAPI 2.6.0
- **GraphQL**: Spring GraphQL
- **Build Tool**: Maven
- **Actuator**: Spring Boot Actuator (for monitoring)

### Frontend
- **Framework**: React 19.1.1
- **Build Tool**: Vite 7.1.7
- **Routing**: React Router v7.9.5
- **State Management**: React Query (@tanstack/react-query 5.90.10)
- **HTTP Client**: Axios 1.13.1
- **Styling**: Tailwind CSS 3.4
- **Offline Storage**: IndexedDB (idb 8.0.3)
- **Package Manager**: npm

---

## 🚀 Quick Start

### Prerequisites

- **Backend**: 
  - Java 21 or higher
  - Maven 3.6+ (or use included `mvnw`)
- **Frontend**: 
  - Node.js 18+ 
  - npm or yarn

### Installation & Running

#### 1. Clone the Repository

```bash
git clone https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8.git
cd APIBP-20242YA-Team-8
```

#### 2. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

**Backend will start on:** `http://localhost:8080`

**Verify it's running:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- GraphiQL: `http://localhost:8080/graphiql`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:vaultifydb`)

#### 3. Start the Frontend

```bash
cd frontend
npm install  # Only needed the first time
npm run dev
```

**Frontend will start on:** `http://localhost:5173`

#### 4. Access the Application

1. Open browser: `http://localhost:5173`
2. **Register** a new account
3. **Login** with your credentials
4. **Create folders** and **upload files**!

---

## 📁 Project Structure

```
vaultify/
├── backend/                    # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/rip/vaultify/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── RateLimitConfig.java
│   │   │   │   │   ├── GraphQlConfig.java
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   ├── controller/     # REST & GraphQL controllers
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── FileController.java
│   │   │   │   │   ├── FolderController.java
│   │   │   │   │   ├── PermissionController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── GraphQLController.java
│   │   │   │   │   └── PreSignedUrlController.java
│   │   │   │   ├── service/         # Business logic
│   │   │   │   │   ├── FileService.java
│   │   │   │   │   ├── FolderService.java
│   │   │   │   │   ├── PermissionService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── PreSignedUrlService.java
│   │   │   │   │   └── IdempotencyService.java
│   │   │   │   ├── repository/      # Data access layer
│   │   │   │   │   ├── FileRepository.java
│   │   │   │   │   ├── FolderRepository.java
│   │   │   │   │   ├── PermissionRepository.java
│   │   │   │   │   └── UserRepository.java
│   │   │   │   ├── model/           # Entity models
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── File.java
│   │   │   │   │   ├── Folder.java
│   │   │   │   │   └── Permission.java
│   │   │   │   ├── security/        # Security components
│   │   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   │   ├── RateLimitFilter.java
│   │   │   │   │   ├── JwtUtil.java
│   │   │   │   │   └── CustomUserDetailsService.java
│   │   │   │   └── dto/             # Data transfer objects
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── graphql/schema.graphqls
│   │   └── test/                    # Test files
│   ├── pom.xml                      # Maven dependencies
│   ├── mvnw                         # Maven wrapper
│   └── uploads/                     # File storage directory
│
├── frontend/                    # React frontend
│   ├── src/
│   │   ├── components/          # React components
│   │   │   ├── ProtectedRoute.jsx
│   │   │   ├── SharingDialog.jsx
│   │   │   ├── SharedFilesNotification.jsx
│   │   │   └── FileCard.jsx
│   │   ├── pages/               # Page components
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   ├── Folders.jsx
│   │   │   ├── Files.jsx
│   │   │   └── OfflineFiles.jsx
│   │   ├── context/             # React context
│   │   │   └── AuthContext.jsx
│   │   ├── hooks/                # Custom hooks
│   │   │   ├── useFolders.js
│   │   │   └── useFiles.js
│   │   ├── services/             # API services
│   │   │   ├── api.js
│   │   │   ├── offlineStorage.js
│   │   │   ├── idempotency.js
│   │   │   └── queryClient.js
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
│
├── docs/                         # Documentation
│   ├── ARCHITECTURE.md          # C4 architecture documentation
│   ├── PRESENTATION_FOLDER_FUNCTIONALITY.md
│   ├── PRESENTATION_RATE_LIMITING.md
│   ├── PROFESSOR_QA.md          # Q&A for presentations
│   ├── c4-diagrams/             # C4 model diagrams (PlantUML)
│   └── Vaultify_API_Collection.postman_collection.json
│
└── README.md                    # This file
```

---

## 📚 API Documentation

### Interactive Documentation

Once the backend is running, access:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
  - Interactive REST API documentation
  - Test endpoints directly from browser
  - Authentication: Click "Authorize" → Enter `Bearer <your-token>`

- **GraphiQL**: `http://localhost:8080/graphiql`
  - Interactive GraphQL playground
  - Test queries and mutations
  - Schema introspection

- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
  - Raw OpenAPI specification
  - Import into Postman/Insomnia

### Postman Collection

Import the complete API collection:
- **Location**: `docs/Vaultify_API_Collection.postman_collection.json`
- **Includes**: All REST and GraphQL endpoints
- **Usage**: Import into Postman for easy testing

### Bruno Collection

Bruno API client collection available in `backend/vaultify/`:
- Pre-configured requests for all endpoints
- Rate limiting test scripts
- Authentication examples

---

## 🔐 Rate Limiting

Vaultify implements **token bucket algorithm-based rate limiting** to protect against abuse and ensure fair resource usage.

### Rate Limit Configuration

| Endpoint Type | Limit | Time Window | Purpose |
|--------------|-------|-------------|---------|
| **Authentication** | 5 requests | 60 seconds | Prevent brute force attacks |
| **File Upload** | 30 requests | 60 seconds | Prevent storage abuse |
| **General API** | 100 requests | 60 seconds | Prevent API abuse |

### Important Notes

- **Login and Register share the same bucket**: Combined limit of 5 requests per 60 seconds
- **Client Identification**: Uses username (authenticated) or IP address (anonymous)
- **Response Headers**: All responses include `X-RateLimit-*` headers

### Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# Authentication endpoints (login/register) - strict limits
ratelimit.auth.requests=5
ratelimit.auth.window=60

# General API endpoints - moderate limits
ratelimit.api.requests=100
ratelimit.api.window=60

# File upload endpoints - conservative limits
ratelimit.upload.requests=30
ratelimit.upload.window=60
```

### Rate Limit Headers

**Success Response:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705852800
X-RateLimit-Type: api
```

**Rate Limit Exceeded (429):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many authentication requests. Please try again later.",
  "retryAfter": 60
}
```

### Testing Rate Limiting

See detailed guides:
- `docs/PRESENTATION_RATE_LIMITING.md` - Complete rate limiting guide
- `backend/vaultify/BRUNO_RATE_LIMIT_TESTING.md` - Bruno testing guide
- `backend/test-login-rate-limit.sh` - Automated test script

---

## 🔒 Security Features

### Authentication
- **JWT Tokens**: Stateless authentication with 24-hour expiration
- **Password Security**: BCrypt hashing (one-way, salted)
- **Token Validation**: Every request validates JWT signature and expiration

### Authorization
- **User Ownership**: All operations verify user ownership
- **Permission Checks**: File sharing requires owner permissions
- **Role-Based Access**: User roles (ROLE_USER) for future expansion

### Protection Mechanisms
- **Rate Limiting**: Prevents brute force and DDoS attacks
- **CORS**: Configured for specific origins (`http://localhost:5173`, `http://localhost:3000`)
- **Input Validation**: Request validation at multiple layers
- **SQL Injection Protection**: JPA parameterized queries

### Security Configuration

**Public Endpoints** (no authentication required):
- `/auth/login`
- `/auth/register`
- `/swagger-ui.html`
- `/graphiql`
- `/h2-console` (development only)

**Protected Endpoints** (JWT required):
- All `/api/**` endpoints
- `/graphql`
- `/auth/me`

---

## 📡 REST API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/auth/register` | Register new user | No |
| `POST` | `/auth/login` | Login user | No |
| `GET` | `/auth/me` | Get current user info | Yes |

### Folders

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/folders` | Create folder | Yes |
| `GET` | `/api/folders` | Get all root folders | Yes |
| `GET` | `/api/folders/{id}` | Get folder by ID | Yes |
| `PUT` | `/api/folders/{id}` | Rename folder | Yes |
| `DELETE` | `/api/folders/{id}` | Delete folder (cascades) | Yes |

### Files

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/files/upload` | Upload file | Yes |
| `GET` | `/api/files/{id}` | Get file details | Yes |
| `GET` | `/api/files/{id}/download` | Download file | Yes |
| `GET` | `/api/files/{id}/preview` | Preview file | Yes |
| `GET` | `/api/files/folder/{folderId}` | Get files in folder | Yes |
| `POST` | `/api/files/copy` | Copy file to folder | Yes |
| `DELETE` | `/api/files/{id}` | Delete file | Yes |

### Permissions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/permissions/share` | Share file with user | Yes |
| `GET` | `/api/permissions/file/{fileId}` | Get file permissions | Yes |
| `GET` | `/api/permissions/shared` | Get files shared with you | Yes |
| `GET` | `/api/permissions/accepted` | Get accepted shared files | Yes |
| `GET` | `/api/permissions/file/{fileId}/owner` | Get file owner | Yes |
| `PUT` | `/api/permissions/{permissionId}` | Update permission | Yes |
| `POST` | `/api/permissions/viewed/{permissionId}` | Mark as viewed | Yes |
| `DELETE` | `/api/permissions/{permissionId}` | Revoke permission | Yes |

### Users

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/users` | Get all users | Yes |

### Pre-Signed URLs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/v1/files/presign/upload` | Generate upload URL | Yes |
| `POST` | `/api/v1/files/presign/download` | Generate download URL | Yes |
| `GET` | `/api/v1/files/presign/read` | Execute download | No (token-based) |

---

## 🔄 GraphQL API

**Endpoint**: `POST /graphql`  
**Authentication**: JWT token in `Authorization: Bearer <token>` header

### Queries

```graphql
# Get current user
{
  currentUser {
    id
    username
    roles
  }
}

# Get all folders
{
  folders {
    id
    name
    parent { id name }
    children { id name }
    files { id originalName }
  }
}

# Get files in folder
{
  filesByFolder(folderId: "1") {
    id
    originalName
    size
    contentType
  }
}

# Get shared files
{
  sharedFiles {
    id
    originalName
    user { username }
  }
}
```

### Mutations

```graphql
# Create folder
mutation {
  createFolder(input: {
    name: "My Documents"
    parentId: null
  }) {
    id
    name
  }
}

# Share file
mutation {
  shareFile(input: {
    fileId: "1"
    username: "otheruser"
    access: READ
  }) {
    id
    access
  }
}
```

**Full GraphQL documentation**: See GraphQL examples section in this README or use GraphiQL at `http://localhost:8080/graphiql`

---

## 🏗 Architecture

Vaultify follows a **3-tier architecture**:

1. **Presentation Layer**: React SPA (frontend)
2. **Application Layer**: Spring Boot API (backend)
3. **Data Layer**: H2/PostgreSQL database + File storage

### Architecture Documentation

Comprehensive C4 model architecture documentation available:

- **Full Architecture**: `docs/ARCHITECTURE.md`
- **C4 Diagrams**: `docs/c4-diagrams/`
  - System Context Diagram
  - Container Diagram
  - Deployment Diagram
  - Component Diagram (API)

**View Diagrams:**
- Online: Copy PlantUML code to [plantuml.com](http://www.plantuml.com/plantuml)
- VS Code: Install PlantUML extension
- Command line: `plantuml docs/c4-diagrams/*.puml`

---

## 💻 Development

### Backend Development

```bash
cd backend

# Run application
./mvnw spring-boot:run
# or
mvn spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# The JAR will be in: target/vaultify-0.0.1-SNAPSHOT.jar
```

### Frontend Development

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Database Access (Development)

**H2 Console**: `http://localhost:8080/h2-console`

**Connection Details:**
- JDBC URL: `jdbc:h2:mem:vaultifydb`
- Username: `sa`
- Password: (empty)

**Note**: H2 is in-memory, data is lost on restart. For production, use PostgreSQL.

---

## 🧪 Testing

### Manual Testing

#### API Testing Tools
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **GraphiQL**: `http://localhost:8080/graphiql`
- **Postman**: Import `docs/Vaultify_API_Collection.postman_collection.json`
- **Bruno**: Use collection in `backend/vaultify/`

#### Rate Limiting Tests

**Quick Test (6 requests):**
```bash
cd backend
./test-login-rate-limit.sh
```

**Python Script:**
```bash
cd backend/vaultify
python3 test-rate-limit.py "your-token" "/api/folders" 101
```

**Bruno**: Use `auth rate-limit-test.bru`

### Automated Testing

**Backend Tests:**
```bash
cd backend
./mvnw test
```

**Frontend Tests:**
- Component tests (to be implemented)
- Manual browser testing

### Testing Documentation

- **Rate Limiting**: `docs/PRESENTATION_RATE_LIMITING.md`
- **Bruno Testing**: `backend/vaultify/BRUNO_RATE_LIMIT_TESTING.md`
- **Complete Testing Guide**: `backend/vaultify/BRUNO_COMPLETE_TESTING_GUIDE.md`

---

## 📖 Documentation

### Architecture & Design
- **ARCHITECTURE.md**: Complete C4 model architecture documentation
- **C4 Diagrams**: PlantUML diagrams for system visualization

### Feature Documentation
- **PRESENTATION_FOLDER_FUNCTIONALITY.md**: Folder feature guide
- **PRESENTATION_RATE_LIMITING.md**: Rate limiting guide
- **BUCKET_EXPLANATION.md**: Token bucket algorithm explanation
- **CACHING_EXPLANATION.md**: Caching strategies

### Testing & Troubleshooting
- **PROFESSOR_QA.md**: Common questions and answers
- **FRONTEND_TROUBLESHOOTING.md**: Frontend issue resolution
- **LOGIN_TROUBLESHOOTING.md**: Login issues guide
- **REGISTRATION_FIX_VERIFICATION.md**: Registration troubleshooting

### API Testing
- **BRUNO_RATE_LIMIT_TESTING.md**: Rate limiting tests in Bruno
- **BRUNO_COMPLETE_TESTING_GUIDE.md**: Complete Bruno testing guide
- **QUICK_START_TESTING.md**: Quick testing reference

---

## 🔧 Configuration

### Backend Configuration

**File**: `backend/src/main/resources/application.properties`

**Key Settings:**
```properties
# JWT Configuration
vaultify.jwt.secret=your-secret-key
vaultify.jwt.expirationMs=86400000  # 24 hours

# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:vaultifydb

# File Upload
spring.servlet.multipart.max-file-size=10MB
file.upload.directory=uploads

# Rate Limiting
ratelimit.auth.requests=5
ratelimit.auth.window=60
ratelimit.api.requests=100
ratelimit.api.window=60
ratelimit.upload.requests=30
ratelimit.upload.window=60
```

### Frontend Configuration

**API Base URL**: `frontend/src/services/api.js`
```javascript
const API_BASE_URL = 'http://localhost:8080';
```

**CORS**: Configured in `SecurityConfig.java` for:
- `http://localhost:5173` (Vite default)
- `http://localhost:3000` (alternative port)

---

## 🚀 Production Deployment

### Current Status
- **Environment**: Development only
- **Database**: H2 in-memory (data lost on restart)
- **File Storage**: Local filesystem
- **Rate Limiting**: In-memory (single server)

### Production Recommendations

1. **Database**: Migrate to PostgreSQL
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost/vaultify
   spring.datasource.driverClassName=org.postgresql.Driver
   ```

2. **File Storage**: Use object storage (AWS S3, Azure Blob, GCS)

3. **Rate Limiting**: Use Redis for distributed rate limiting

4. **Secrets Management**: Move JWT secret to environment variables or secrets manager

5. **HTTPS**: Enable TLS/SSL certificates

6. **Monitoring**: Set up application monitoring (Prometheus, Grafana)

7. **Logging**: Centralized logging (ELK stack, CloudWatch)

See `docs/ARCHITECTURE.md` for detailed deployment recommendations.

---

## 🐛 Troubleshooting

### Common Issues

**Backend won't start:**
- Check Java version: `java -version` (should be 21+)
- Check port 8080 is available: `lsof -i :8080`
- Check Maven dependencies: `mvn clean install`

**Frontend won't start:**
- Check Node.js version: `node -version` (should be 18+)
- Reinstall dependencies: `rm -rf node_modules && npm install`
- Check port 5173 is available

**Rate limiting not working:**
- Restart backend (clears cache)
- Check `application.properties` configuration
- Verify filter is in filter chain

**CORS errors:**
- Check frontend URL matches CORS config
- Verify `SecurityConfig.java` CORS settings
- Restart backend after CORS changes

**See detailed guides:**
- `FRONTEND_TROUBLESHOOTING.md`
- `LOGIN_TROUBLESHOOTING.md`
- `REGISTRATION_FIX_VERIFICATION.md`

---

## 📝 API Request Examples

### Authentication

**Register:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInMs": 86400000
}
```

### Folders

**Create Folder:**
```bash
curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Documents","parentId":null}'
```

**Get Folders:**
```bash
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Files

**Upload File:**
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/file.pdf" \
  -F "folderId=1"
```

**Download File:**
```bash
curl -X GET http://localhost:8080/api/files/1/download \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o downloaded-file.pdf
```

### Permissions

**Share File:**
```bash
curl -X POST http://localhost:8080/api/permissions/share \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fileId":1,"username":"otheruser","access":"READ"}'
```

---

## 🎯 Key Features Summary

| Feature | Status | Description |
|---------|--------|-------------|
| **Authentication** | ✅ | JWT-based login/register |
| **Folder Management** | ✅ | Hierarchical folders with CRUD |
| **File Management** | ✅ | Upload, download, preview, delete |
| **File Sharing** | ✅ | Share with permissions (READ/WRITE/OWNER) |
| **Rate Limiting** | ✅ | Token bucket algorithm (3 tiers) |
| **REST API** | ✅ | Complete RESTful API |
| **GraphQL API** | ✅ | Full GraphQL support |
| **Offline Storage** | ✅ | IndexedDB for offline access |
| **ETag Caching** | ✅ | HTTP caching for performance |
| **API Documentation** | ✅ | Swagger UI + GraphiQL |
| **Pre-Signed URLs** | ✅ | Secure file access URLs |
| **Idempotency** | ✅ | Prevent duplicate operations |

---

## 📚 Additional Resources

### Learning Resources
- **Rate Limiting**: `docs/PRESENTATION_RATE_LIMITING.md`
- **Folder Functionality**: `docs/PRESENTATION_FOLDER_FUNCTIONALITY.md`
- **Architecture**: `docs/ARCHITECTURE.md`
- **Q&A**: `docs/PROFESSOR_QA.md`

### Testing Resources
- **Bruno Collection**: `backend/vaultify/*.bru`
- **Postman Collection**: `docs/Vaultify_API_Collection.postman_collection.json`
- **Test Scripts**: `backend/test-login-rate-limit.sh`, `backend/vaultify/test-rate-limit.py`

---

## 🤝 Contributing

This is a team project. For contributions:

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request

---

## 📄 License

[Your License Here]

---

## 👥 Team

- @harshithcodes
- @VANSHIKAJAIN01
- @LOKESHPRASAD12
- @aditipandey16
- @Abhi792002
- @lokesh-sap

---

## 🔗 Links

- **Repository**: [GitHub](https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8)
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (when running)
- **GraphiQL**: `http://localhost:8080/graphiql` (when running)

---

## 📞 Support

For issues or questions:
1. Check troubleshooting guides in `docs/`
2. Review Q&A document: `docs/PROFESSOR_QA.md`
3. Check architecture documentation: `docs/ARCHITECTURE.md`

---

**Built with ❤️ by Team 8**
