# Frontend Setup Guide

## Quick Start

### 1. Start the Backend
```bash
# In the backend directory
cd backend
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

The backend will run on `http://localhost:8080`

### 2. Start the Frontend
```bash
cd frontend
npm install  # Only needed the first time
npm run dev
```

The frontend will run on `http://localhost:5173`

### 3. Access the Application
- Open `http://localhost:5173` in your browser
- Register a new account or login
- Start creating folders and uploading files!

## Application Features

### ✅ Implemented Features

1. **Authentication**
   - Login page with username/password
   - Register page for new users
   - JWT token management with automatic logout on 401 errors
   - Protected routes requiring authentication

2. **Folder Management**
   - View all folders in a grid layout
   - Create new folders
   - Delete folders (with confirmation)
   - Click on a folder to navigate to its files
   - Settings button on folder cards to open sharing dialog

3. **File Management**
   - View all files in a selected folder
   - Upload files via drag-and-drop or file picker
   - Download files
   - Delete files (with confirmation)
   - View file metadata (size, upload date)
   - Settings button on file cards to open sharing dialog
   - Back button to return to folders list

4. **Sharing Dialog UI**
   - Modal dialog accessible from folder and file cards
   - Select multiple files from the folder
   - Select multiple users from dropdown
   - UI ready for backend integration

### ⚠️ Pending Backend Implementation

The sharing feature UI is complete, but requires backend endpoints:
- `GET /api/users` - To fetch list of users for sharing
- `POST /api/share` - To actually share files/folders

See `BACKEND_ENDPOINTS_NEEDED.md` for details.

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── ProtectedRoute.jsx    # Route guard for authenticated pages
│   │   └── SharingDialog.jsx      # Modal for sharing files/folders
│   ├── context/
│   │   └── AuthContext.jsx        # Authentication state management
│   ├── pages/
│   │   ├── Login.jsx              # Login page
│   │   ├── Register.jsx          # Registration page
│   │   ├── Folders.jsx           # Folder listing page
│   │   └── Files.jsx             # File listing page (per folder)
│   ├── services/
│   │   └── api.js                # API service layer with axios
│   ├── App.jsx                   # Main app with routing
│   ├── main.jsx                  # Entry point
│   └── index.css                 # Global styles with Tailwind
├── package.json
├── vite.config.js
├── tailwind.config.js
└── postcss.config.js
```

## API Integration

All API calls are centralized in `src/services/api.js`:
- Automatic JWT token injection in headers
- Automatic redirect to login on 401 errors
- Base URL configuration for easy environment switching

## Styling

The application uses Tailwind CSS for styling:
- Modern, responsive design
- Gradient backgrounds
- Smooth transitions and hover effects
- Card-based layouts for folders and files

## Routing

- `/login` - Login page (redirects to `/folders` if already logged in)
- `/register` - Registration page (redirects to `/folders` if already logged in)
- `/folders` - Folder listing (protected, requires auth)
- `/folders/:folderId/files` - File listing for a specific folder (protected)
- `/` - Redirects to `/folders`

## Development Tips

1. **API Base URL**: Modify `src/services/api.js` if your backend runs on a different port
2. **CORS**: Backend CORS is configured for `localhost:5173` and `localhost:3000`
3. **Hot Reload**: Vite provides instant hot module replacement
4. **Browser DevTools**: Check Network tab to see API calls and responses

## Troubleshooting

### CORS Errors
- Ensure backend CORS configuration includes your frontend URL
- Check that backend is running on `http://localhost:8080`

### 401 Unauthorized
- Token might be expired or invalid
- Frontend will automatically redirect to login
- Try logging in again

### Users Not Loading in Sharing Dialog
- The `/api/users` endpoint doesn't exist yet
- See `BACKEND_ENDPOINTS_NEEDED.md` for implementation guide
- The dialog will show an informative error message

### Files Not Uploading
- Check file size limits (backend default: 10MB)
- Ensure folder ID is valid
- Check backend logs for errors

## Next Steps

1. Implement `/api/users` endpoint in backend
2. Implement sharing/permissions model and `/api/share` endpoint
3. Add user ownership tracking to folders and files
4. Filter folders/files by ownership/sharing permissions
5. Add permission levels (read-only, read-write) to sharing
