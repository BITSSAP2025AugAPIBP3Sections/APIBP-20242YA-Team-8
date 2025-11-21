import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../services/api';
import { queryClient } from '../services/queryClient';

const AuthContext = createContext(null);

const USER_STORAGE_KEY = 'vaultify_user';
const TOKEN_STORAGE_KEY = 'token';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  // Restore user from localStorage on mount
  useEffect(() => {
    const storedUser = localStorage.getItem(USER_STORAGE_KEY);
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    
    if (storedUser && token) {
      try {
        const userData = JSON.parse(storedUser);
        setUser(userData);
        // Try to verify with server, but don't fail if offline
        if (navigator.onLine) {
          fetchUser();
        } else {
          setLoading(false);
        }
      } catch (err) {
        console.error('Failed to parse stored user:', err);
        setLoading(false);
      }
    } else {
      setLoading(false);
    }

    // Listen for online/offline events
    const handleOnline = () => {
      setIsOffline(false);
      // Try to refresh user if we have a token
      if (localStorage.getItem(TOKEN_STORAGE_KEY)) {
        fetchUser();
      }
    };
    const handleOffline = () => setIsOffline(true);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const fetchUser = async () => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      setLoading(false);
      return;
    }

    try {
      const response = await authAPI.getMe();
      const userData = response.data;
      setUser(userData);
      // Persist user to localStorage
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData));
      setLoading(false);
    } catch (error) {
      // Only remove token if it's an auth error (401), not network error
      if (error.response?.status === 401) {
        // Clear cache on unauthorized (session expired)
        queryClient.clear();
        if (typeof window !== 'undefined' && window.etagCache) {
          window.etagCache.clear();
        }
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
        setUser(null);
      } else {
        // Network error - keep user from localStorage
        console.warn('Network error, using cached user data');
      }
      setLoading(false);
    }
  };

  const login = async (username, password) => {
    try {
      // Clear all cache before login to ensure fresh data for new user
      queryClient.clear();
      // Also clear ETag cache (exposed via window in api.js)
      if (typeof window !== 'undefined' && window.etagCache) {
        window.etagCache.clear();
      }
      
      const response = await authAPI.login(username, password);
      if (!response || !response.data) {
        throw new Error('Invalid response from server');
      }
      const { token } = response.data;
      if (!token) {
        throw new Error('No token received from server');
      }
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
      await fetchUser();
      return response.data;
    } catch (error) {
      // Re-throw with better error message
      if (error.response) {
        // Server responded with error
        const errorMessage = error.response.data?.error || error.response.statusText || 'Login failed';
        throw new Error(errorMessage);
      } else if (error.request) {
        // Request made but no response (network error)
        throw new Error('Network error: Could not connect to server. Please check your connection.');
      } else {
        // Something else happened
        throw error;
      }
    }
  };

  const register = async (username, password) => {
    try {
      const response = await authAPI.register(username, password);
      if (!response || !response.data) {
        throw new Error('Invalid response from server');
      }
      return response.data;
    } catch (error) {
      // Re-throw with better error message
      if (error.response) {
        // Server responded with error
        const errorMessage = error.response.data?.error || error.response.statusText || 'Registration failed';
        throw new Error(errorMessage);
      } else if (error.request) {
        // Request made but no response (network error)
        throw new Error('Network error: Could not connect to server. Please check your connection.');
      } else {
        // Something else happened
        throw error;
      }
    }
  };

  const logout = () => {
    // Clear all React Query cache
    queryClient.clear();
    // Clear ETag cache
    if (typeof window !== 'undefined' && window.etagCache) {
      window.etagCache.clear();
    }
    // Clear localStorage
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isOffline }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
