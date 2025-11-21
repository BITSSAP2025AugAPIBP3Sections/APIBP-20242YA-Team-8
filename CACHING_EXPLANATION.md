# Redis Caching Implementation - What We're Caching

## What IS Being Cached (Metadata Only)

### 1. Folder Metadata (`userFolders` cache)
- **What**: List of folders for a user
- **Contains**: 
  - Folder ID, name
  - Parent folder info (if any)
  - Child folders info
- **Cache Key**: `v1::userFolders::user:{userId}`
- **TTL**: 10 minutes
- **Example**: When you call `GET /api/folders`, the folder list is cached

### 2. File Metadata (`folderFiles` cache)
- **What**: List of files in a folder for a specific user
- **Contains**:
  - File ID, name, size, content type
  - Upload date
  - Folder info
  - Sharing/permission info
  - Owner info
- **Cache Key**: `v1::folderFiles::folder:{folderId}:user:{userId}`
- **TTL**: 2 minutes
- **Example**: When you open a folder and see the file list, that list is cached

### 3. Presigned URL Tokens
- **What**: Temporary tokens for file upload/download
- **TTL**: 60 seconds
- **Cache Key**: `presigned:{token}`

## What is NOT Being Cached

❌ **File Content**: The actual file bytes are NOT cached
- Files are stored on disk and served directly
- Only metadata (name, size, type) is cached

## How Caching Works

### First Request (Cache Miss)
1. User requests `/api/folders`
2. Backend queries database
3. Results are cached in Redis
4. Response returned to user
5. **Log shows**: `❌ CACHE MISS` → `📝 Fetching from database` → `💾 CACHE WRITE`

### Subsequent Requests (Cache Hit)
1. User requests `/api/folders` again (within 10 minutes)
2. Backend checks Redis cache
3. Data found in cache
4. Response returned from cache (no database query!)
5. **Log shows**: `✅ CACHE HIT`

## Benefits

- **Faster Response Times**: No database query needed for cached data
- **Reduced Database Load**: Fewer queries to the database
- **Better Scalability**: Can handle more concurrent users

## How to Verify Caching is Working

1. **Check Backend Logs**:
   - First request: `❌ CACHE MISS` → `💾 CACHE WRITE`
   - Second request: `✅ CACHE HIT`

2. **Check RedisInsight**:
   - Look for keys like `v1::userFolders::user:1`
   - Look for keys like `v1::folderFiles::folder:1:user:1`

3. **Use Cache Stats API**:
   ```
   GET http://localhost:8080/api/cache/stats
   Authorization: Bearer <token>
   ```

4. **Test Performance**:
   - First request: Slower (database query)
   - Second request: Faster (from cache)

## Cache Invalidation

Cache is automatically cleared when:
- A new folder is created
- A folder is renamed or deleted
- A file is uploaded or deleted
- A file is shared

This ensures users always see up-to-date data.

