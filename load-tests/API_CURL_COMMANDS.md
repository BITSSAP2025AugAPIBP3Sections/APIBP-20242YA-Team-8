# Vaultify API - Complete List with cURL Commands

Base URL: `http://localhost:8080`

## Authentication Endpoints

### 1. Register User
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 2. Login User
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Response includes token:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresInMs": 86400000
}
```

### 3. Get Current User (Me)
```bash
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## User Endpoints

### 4. Get All Users
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## Folder Endpoints

### 5. Create Folder
```bash
curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Documents",
    "parentId": null
  }'
```

### 6. Get All Folders
```bash
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 7. Get Folder by ID
```bash
curl -X GET http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 8. Update/Rename Folder
```bash
curl -X PUT http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Folder Name"
  }'
```

### 9. Delete Folder
```bash
curl -X DELETE http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## File Endpoints

### 10. Upload File
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -F "file=@/path/to/your/file.txt" \
  -F "folderId=1"
```

### 11. Get Files by Folder
```bash
curl -X GET http://localhost:8080/api/files/folder/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 12. Get File by ID
```bash
curl -X GET http://localhost:8080/api/files/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 13. Preview File
```bash
curl -X GET http://localhost:8080/api/files/1/preview \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 14. Download File
```bash
curl -X GET http://localhost:8080/api/files/1/download \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -o downloaded_file.txt
```

### 15. Delete File
```bash
curl -X DELETE http://localhost:8080/api/files/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 16. Copy Shared File
```bash
curl -X POST http://localhost:8080/api/files/copy \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": 1,
    "folderId": 2
  }'
```

---

## Permission/Sharing Endpoints

### 17. Share File
```bash
curl -X POST http://localhost:8080/api/permissions/share \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "1",
    "username": "anotheruser",
    "access": "READ"
  }'
```

**Access types:** `READ`, `WRITE`

### 18. Get File Permissions
```bash
curl -X GET http://localhost:8080/api/permissions/file/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 19. Get Shared Files
```bash
curl -X GET http://localhost:8080/api/permissions/shared \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 20. Get Accepted Shared Files
```bash
curl -X GET http://localhost:8080/api/permissions/accepted \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 21. Get File Owner
```bash
curl -X GET http://localhost:8080/api/permissions/file/1/owner \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 22. Mark Permission as Viewed
```bash
curl -X POST http://localhost:8080/api/permissions/viewed/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 23. Update Permission
```bash
curl -X PUT http://localhost:8080/api/permissions/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "access": "WRITE"
  }'
```

### 24. Revoke Permission
```bash
curl -X DELETE http://localhost:8080/api/permissions/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## GraphQL Endpoint

### 25. GraphQL Query/Mutation
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { currentUser { id username roles } }"
  }'
```

**Example GraphQL Queries:**

#### Get Current User
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { currentUser { id username roles } }"
  }'
```

#### Get All Folders
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { folders { id name parent { id name } user { id username } } }"
  }'
```

#### Get All Files
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { files { id originalName contentType size uploadedAt folder { id name } user { id username } } }"
  }'
```

#### Create Folder (Mutation)
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation($input: CreateFolderInput!) { createFolder(input: $input) { id name parent { id name } user { id username } } }",
    "variables": {
      "input": {
        "name": "GraphQL Test Folder",
        "parentId": null
      }
    }
  }'
```

#### Get Permissions
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { permissions { id access viewed file { id originalName } user { id username } } }"
  }'
```

---

## Notes

1. **Authentication**: Most endpoints require a Bearer token in the Authorization header
2. **Token**: Get token from `/auth/login` endpoint
3. **Base URL**: Replace `http://localhost:8080` with your server URL if different
4. **File Upload**: Use `-F` flag for multipart/form-data file uploads
5. **IDs**: Replace numeric IDs (like `1`, `2`) with actual IDs from your system

## Testing with Token

To test with a token, first login and save the token:

```bash
# Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.token')

# Use token in subsequent requests
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer $TOKEN"
```

