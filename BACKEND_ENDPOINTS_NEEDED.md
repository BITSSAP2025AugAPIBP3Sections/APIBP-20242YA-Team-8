# Backend Endpoints Needed for Complete Frontend Functionality

The frontend has been fully implemented with all UI components. However, there are a few backend endpoints that need to be added to complete the sharing feature:

## Required Endpoints

### 1. Get All Users (`GET /api/users`)

**Purpose**: Used by the sharing dialog to populate the user dropdown for selecting users to share files/folders with.

**Location**: Should be added to a new `UserController` or existing controller.

**Expected Response**:
```json
[
  {
    "id": 1,
    "username": "user1"
  },
  {
    "id": 2,
    "username": "user2"
  }
]
```

**Security**: Should only return users that the current authenticated user is allowed to see (typically all users except maybe sensitive admin accounts).

**Implementation Example**:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll()
            .stream()
            .map(user -> Map.of(
                "id", user.getId(),
                "username", user.getUsername()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
```

### 2. Share Files/Folders (`POST /api/share`)

**Purpose**: Handle the actual sharing of files and folders with selected users.

**Request Body**:
```json
{
  "folderId": 1,
  "fileIds": [10, 11, 12],  // Optional: specific files to share, or empty array for all files
  "userIds": [2, 3]          // Users to share with
}
```

**Response**: 
```json
{
  "success": true,
  "message": "Files shared successfully"
}
```

**Implementation Notes**:
- You'll need to create a sharing/permissions model/table to track which users have access to which files/folders
- Consider permission levels: read-only, read-write, etc.
- The sharing relationship should be stored in the database

### Optional: Enhanced Sharing Endpoints

1. **Get Shared Files/Folders** (`GET /api/shared`) - For users to see files/folders shared with them
2. **Get Sharing Info** (`GET /api/folders/{id}/shares` or `/api/files/{id}/shares`) - See who a folder/file is shared with
3. **Remove Share** (`DELETE /api/share/{shareId}`) - Remove sharing permission

## Security Considerations

1. Users should only be able to share files/folders they own
2. Users should only see files/folders shared with them
3. The `/api/users` endpoint should not expose sensitive information (password hashes, etc.)
4. Validate that the current user has permission to share the specified folder/files

## Database Schema Suggestion

You may want to add a sharing/permissions table:

```sql
CREATE TABLE file_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT,
    folder_id BIGINT,
    shared_with_user_id BIGINT,
    shared_by_user_id BIGINT,
    permission_level VARCHAR(50) DEFAULT 'READ',  -- READ, WRITE, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_with_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Current Frontend Behavior

The frontend sharing dialog will currently:
- Show an alert with the data that would be sent when the share button is clicked
- Attempt to fetch users from `/api/users` (will show an error if endpoint doesn't exist)
- Ready to send sharing requests once the backend endpoint is implemented

Once these endpoints are added, the sharing feature will be fully functional!
