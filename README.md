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

**API Documentation (Swagger UI)**: Once the backend is running, you can access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

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
- **Sharing UI**: Dialog interface for sharing files/folders (backend endpoints needed)

### 🚧 Pending

- User sharing endpoints (see `BACKEND_ENDPOINTS_NEEDED.md`)
- Permission management system
- Shared files/folders view

## Documentation

- **API Documentation**: Interactive Swagger UI available at `http://localhost:8080/swagger-ui/index.html` when the backend is running. You can test all endpoints directly from the UI. To authenticate, click the "Authorize" button and enter your JWT token in the format: `Bearer <your_token>`
- **Frontend Setup**: See `FRONTEND_SETUP.md` for detailed frontend documentation
- **Backend Endpoints**: See `BACKEND_ENDPOINTS_NEEDED.md` for required backend implementation

## Tech Stack

### Backend
- Spring Boot 3.5.6
- Spring Security with JWT
- Springdoc OpenAPI (Swagger UI)
- H2 Database (in-memory)
- Maven

### Frontend
- React 19
- React Router v7
- Tailwind CSS 3.4
- Axios
- Vite

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
