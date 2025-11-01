import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle unauthorized responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  register: (username, password) =>
    api.post('/auth/register', { username, password }),
  login: (username, password) =>
    api.post('/auth/login', { username, password }),
  getMe: () => api.get('/auth/me'),
};

export const folderAPI = {
  getAll: () => api.get('/api/folders'),
  getById: (id) => api.get(`/api/folders/${id}`),
  create: (name, parentId) =>
    api.post('/api/folders', { name, parentId: parentId || null }),
  update: (id, name) => api.put(`/api/folders/${id}`, { name }),
  delete: (id) => api.delete(`/api/folders/${id}`),
};

export const fileAPI = {
  upload: (file, folderId) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folderId', folderId);
    return api.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  getByFolder: (folderId) => api.get(`/api/files/folder/${folderId}`),
  getById: (id) => api.get(`/api/files/${id}`),
  delete: (id) => api.delete(`/api/files/${id}`),
  download: (id) => api.get(`/api/files/${id}/download`, { responseType: 'blob' }),
};

// Note: This endpoint doesn't exist yet in the backend
// You'll need to add it to UserController
export const userAPI = {
  getAll: () => api.get('/api/users'),
};

export default api;
