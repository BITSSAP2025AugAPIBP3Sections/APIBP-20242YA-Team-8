import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState, useEffect } from 'react';
import { getAllOfflineFiles } from '../services/offlineStorage';

const ProtectedRoute = ({ children }) => {
  const { user, loading, isOffline } = useAuth();
  const [hasOfflineFiles, setHasOfflineFiles] = useState(false);
  const [checkingOffline, setCheckingOffline] = useState(true);

  useEffect(() => {
    const checkOfflineFiles = async () => {
      try {
        const files = await getAllOfflineFiles();
        setHasOfflineFiles(files.length > 0);
      } catch (err) {
        if (err.name !== 'VersionError') {
          console.error('Failed to check offline files:', err);
        }
        setHasOfflineFiles(false);
      } finally {
        setCheckingOffline(false);
      }
    };
    checkOfflineFiles();
  }, []);

  if (loading || checkingOffline) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (!user) {
    if (hasOfflineFiles) {
      return (
        <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <svg className="w-6 h-6 text-blue-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <div>
                    <h3 className="text-lg font-semibold text-blue-900 mb-1">
                      {isOffline ? 'You are offline' : 'Session expired'}
                    </h3>
                    <p className="text-blue-700 mb-4">
                      {isOffline 
                        ? 'You have files available offline. You can view and download them without internet connection.'
                        : 'You have files available offline. Login to access all features, or view your offline files.'}
                    </p>
                    <div className="flex gap-3">
                      <a
                        href="/offline"
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors inline-block"
                      >
                        View Offline Files
                      </a>
                      <a
                        href="/login"
                        className="px-4 py-2 bg-white text-blue-600 border border-blue-600 rounded-lg hover:bg-blue-50 transition-colors inline-block"
                      >
                        Login
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <Navigate to="/login" replace />
          </div>
        </div>
      );
    }
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default ProtectedRoute;
