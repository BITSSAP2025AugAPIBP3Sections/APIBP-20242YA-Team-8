# Vaultify

A modern file storage and sharing application built with Spring Boot (backend) and React (frontend).

## Project Structure

```
vaultify/
├── backend/          # Spring Boot backend application
│   ├── src/         # Java source code
│   ├── pom.xml      # Maven configuration
│   ├── mvnw         # Maven wrapper
│   └── ...
├── frontend/        # React frontend application
│   ├── src/         # React source code
│   ├── package.json # Node.js dependencies
│   └── ...
├── README.md        # This file
├── FRONTEND_SETUP.md
└── BACKEND_ENDPOINTS_NEEDED.md
```

## Quick Start

### Prerequisites

- **Backend**: Java 21, Maven
- **Frontend**: Node.js 18+, npm

### Running the Application

#### 1. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

#### 2. Start the Frontend

```bash
cd frontend
npm install  # Only needed the first time
npm run dev
```

The frontend will start on `http://localhost:5173`

### Access the Application

Open your browser and navigate to `http://localhost:5173`

1. Register a new account
2. Login
3. Create folders and upload files!

## Features

### ✅ Implemented

- **Authentication**: JWT-based login and registration
- **Folder Management**: Create, view, and delete folders
- **File Management**: Upload, download, view, and delete files
- **Sharing UI**: Dialog interface for sharing files/folders
- **REST API**: Complete REST endpoints for all operations
- **GraphQL API**: Comprehensive GraphQL schema and resolvers
- **API Documentation**: Swagger/OpenAPI documentation
- **Permission System**: File sharing with READ/WRITE/OWNER access levels

### 🚧 Pending

- User sharing endpoints (see `BACKEND_ENDPOINTS_NEEDED.md`)
- Permission management system
- Shared files/folders view

## Documentation

- **Frontend Setup**: See `FRONTEND_SETUP.md` for detailed frontend documentation
- **Backend Endpoints**: See `BACKEND_ENDPOINTS_NEEDED.md` for required backend implementation

## Tech Stack

### Backend
- Spring Boot 3.5.6
- Spring Security with JWT
- H2 Database (in-memory)
- Maven

### Frontend
- React 19
- React Router v7
- Tailwind CSS 3.4
- Axios
- Vite

## API Documentation

### Interactive Documentation

Once the backend is running, you can access:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` - Interactive REST API documentation
- **GraphiQL**: `http://localhost:8080/graphiql` - Interactive GraphQL playground
- **OpenAPI JSON**: `http://localhost:8080/api-docs` - Raw OpenAPI specification

### Postman Collection

Import the complete API collection from `docs/Vaultify_API_Collection.postman_collection.json` into Postman for testing both REST and GraphQL endpoints.

### GraphQL API

**Endpoint**: `POST /graphql`  
**Authentication**: Requires JWT token in Authorization header

#### 🔐 Authentication
All GraphQL queries and mutations require a valid JWT token:
```bash
Authorization: Bearer <your-jwt-token>
```

#### 📋 Query Examples

##### User Queries
```graphql
# Get all users
{
  users {
    id
    username
    roles
  }
}

# Get current logged-in user
{
  currentUser {
    id
    username
    roles
  }
}
```

##### File Queries
```graphql
# Get all files with detailed information
{
  files {
    id
    originalName
    storedName
    contentType
    size
    filePath
    uploadedAt
    user {
      id
      username
    }
    folder {
      id
      name
    }
  }
}

# Get specific file by ID
{
  file(id: "1") {
    id
    originalName
    size
    contentType
    uploadedAt
    user {
      username
    }
  }
}

# Get files in a specific folder
{
  filesByFolder(folderId: "2") {
    id
    originalName
    size
    user {
      username
    }
  }
}
```

##### Folder Queries
```graphql
# Get all folders with nested structure
{
  folders {
    id
    name
    user {
      username
    }
    parent {
      id
      name
    }
    children {
      id
      name
    }
    files {
      id
      originalName
    }
  }
}

# Get root folders (no parent)
{
  rootFolders {
    id
    name
    children {
      id
      name
      files {
        id
        originalName
      }
    }
  }
}
```

##### Permission Queries
```graphql
# Get all permissions
{
  permissions {
    id
    access
    viewed
    file {
      id
      originalName
    }
    user {
      id
      username
    }
  }
}

# Get permissions for a specific file
{
  filePermissions(fileId: "1") {
    id
    access
    viewed
    user {
      username
    }
  }
}

# Get current user's permissions
{
  userPermissions {
    id
    access
    viewed
    file {
      id
      originalName
      contentType
    }
  }
}

# Get files shared with current user
{
  sharedFiles {
    id
    originalName
    size
    contentType
    user {
      username
    }
  }
}
```

#### 🔄 Mutation Examples

##### Folder Mutations
```graphql
# Create a new folder
mutation {
  createFolder(input: {
    name: "My Documents"
    parentId: null
  }) {
    id
    name
    user {
      username
    }
  }
}

# Create a subfolder
mutation {
  createFolder(input: {
    name: "Subfolder"
    parentId: "1"
  }) {
    id
    name
    parent {
      id
      name
    }
  }
}

# Update folder name
mutation {
  updateFolder(id: "1", name: "Updated Folder Name") {
    id
    name
  }
}

# Delete folder
mutation {
  deleteFolder(id: "1")
}
```

##### Permission Mutations
```graphql
# Share a file with another user
mutation {
  shareFile(input: {
    fileId: "1"
    username: "otheruser"
    access: READ
  }) {
    id
    access
    file {
      originalName
    }
    user {
      username
    }
  }
}

# Update permission access level
mutation {
  updatePermission(id: "1", input: {
    access: WRITE
  }) {
    id
    access
    file {
      originalName
    }
  }
}

# Remove permission
mutation {
  removePermission(id: "1")
}

# Mark permission as viewed
mutation {
  markAsViewed(permissionId: "1") {
    id
    viewed
    file {
      originalName
    }
  }
}
```

#### 🔧 Advanced Query Examples

##### Complex Nested Queries
```graphql
{
  currentUser {
    id
    username
    roles
  }
  rootFolders {
    id
    name
    children {
      id
      name
      files {
        id
        originalName
        size
      }
    }
  }
  sharedFiles {
    id
    originalName
    user {
      username
    }
  }
  userPermissions {
    id
    access
    viewed
    file {
      originalName
    }
  }
}
```

##### Schema Introspection
```graphql
# Get schema information
{
  __schema {
    queryType { name }
    mutationType { name }
    subscriptionType { name }
  }
}
```

#### 📋 Testing GraphQL API

##### Using cURL:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"query":"{ users { id username roles } }"}'
```

##### Using Postman:
1. Method: POST
2. URL: `http://localhost:8080/graphql`
3. Headers:
   - `Content-Type: application/json`
   - `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body (raw JSON):
```json
{
  "query": "{ currentUser { id username roles } }"
}
```

##### Access Levels:
- `READ`: Can view file
- `WRITE`: Can modify file
- `OWNER`: Full control over file

### REST API Endpoints

#### Authentication
- `POST /api/auth/register` - Register user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user

#### Files & Folders
- `POST /api/files/upload` - Upload file
- `GET /api/files/{id}` - Get file details
- `GET /api/files/{id}/download` - Download file
- `DELETE /api/files/{id}` - Delete file
- `POST /api/folders` - Create folder
- `GET /api/folders` - Get folders
- `PUT /api/folders/{id}` - Update folder
- `DELETE /api/folders/{id}` - Delete folder

#### Permissions
- `POST /api/permissions/share` - Share file
- `GET /api/permissions/file/{fileId}` - Get file permissions
- `PUT /api/permissions/{id}` - Update permission
- `DELETE /api/permissions/{id}` - Remove permission

## Development

### Backend Development

From the `backend/` directory:
- Run tests: `./mvnw test`
- Build: `./mvnw clean package`
- Run: `./mvnw spring-boot:run`

### Frontend Development

From the `frontend/` directory:
- Install dependencies: `npm install`
- Start dev server: `npm run dev`
- Build: `npm run build`
- Preview build: `npm run preview`

## License

[Your License Here]
