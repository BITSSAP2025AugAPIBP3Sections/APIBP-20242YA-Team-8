# Vaultify Frontend

React frontend application for Vaultify - a file storage and sharing application.

## Features

- **Authentication**: Login and Register pages with JWT token management
- **Folder Management**: View, create, and delete folders
- **File Management**: Upload, view, download, and delete files within folders
- **Sharing Dialog**: UI for sharing files and folders with other users (backend endpoint needed)

## Tech Stack

- React 19
- React Router v7
- Axios for API calls
- Tailwind CSS for styling
- Vite as build tool

## Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn
- Backend API running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will be available at `http://localhost:5173` (or another port if 5173 is in use).

### Build for Production

```bash
npm run build
```

The production build will be in the `dist` directory.

## Project Structure

```
frontend/
├── src/
│   ├── components/       # Reusable components
│   │   ├── ProtectedRoute.jsx
│   │   └── SharingDialog.jsx
│   ├── context/          # React contexts
│   │   └── AuthContext.jsx
│   ├── pages/           # Page components
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Folders.jsx
│   │   └── Files.jsx
│   ├── services/        # API services
│   │   └── api.js
│   ├── App.jsx          # Main app component with routing
│   ├── main.jsx         # Entry point
│   └── index.css        # Global styles
├── package.json
└── vite.config.js
```

## API Endpoints

The frontend expects the following backend endpoints:

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login user
- `GET /auth/me` - Get current user info

### Folders
- `GET /api/folders` - Get all folders
- `GET /api/folders/:id` - Get folder by ID
- `POST /api/folders` - Create folder
- `PUT /api/folders/:id` - Update folder
- `DELETE /api/folders/:id` - Delete folder

### Files
- `POST /api/files/upload` - Upload file
- `GET /api/files/folder/:folderId` - Get files in folder
- `GET /api/files/:id` - Get file by ID
- `DELETE /api/files/:id` - Delete file
- `GET /api/files/:id/download` - Download file

### Users (Required for Sharing Feature)
- `GET /api/users` - Get all users (⚠️ **Not yet implemented in backend**)

## Notes

### Sharing Feature

The sharing dialog UI is fully implemented, but it requires a backend endpoint that doesn't exist yet. To complete the sharing feature, you'll need to:

1. Create a `/api/users` endpoint in the backend to get all users
2. Create a sharing/permission endpoint (e.g., `POST /api/share`) to handle file/folder sharing

The sharing dialog currently shows a placeholder alert when the share button is clicked, demonstrating what data would be sent to the backend.

## Development

### Environment Variables

You can customize the API base URL by modifying `src/services/api.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080';
```

### CORS Configuration

Ensure your Spring Boot backend allows CORS requests from the frontend. You may need to add CORS configuration to `SecurityConfig.java`.