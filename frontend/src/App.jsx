import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Folders from './pages/Folders';
import Files from './pages/Files';
import OfflineFiles from './pages/OfflineFiles';

const AppRoutes = () => {
  const { user } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/folders" replace /> : <Login />}
      />
      <Route
        path="/register"
        element={user ? <Navigate to="/folders" replace /> : <Register />}
      />
      <Route
        path="/folders"
        element={
          <ProtectedRoute>
            <Folders />
          </ProtectedRoute>
        }
      />
      <Route
        path="/folders/:folderId/files"
        element={
          <ProtectedRoute>
            <Files />
          </ProtectedRoute>
        }
      />
      <Route
        path="/offline"
        element={<OfflineFiles />}
      />
      <Route path="/" element={<Navigate to="/folders" replace />} />
    </Routes>
  );
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}

export default App;