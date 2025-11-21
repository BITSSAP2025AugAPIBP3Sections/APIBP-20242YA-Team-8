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

const PRESIGN_BASE_PATH = '/api/v1/files/presign';

const requestPreSignedUrl = (path, payload) =>
  api.post(`${PRESIGN_BASE_PATH}/${path}`, payload);

export const fileAPI = {
  upload: async (file, folderId, fileId = null) => {
    const resolvedFolderId =
      folderId !== undefined && folderId !== null ? Number(folderId) : null;

    if (!resolvedFolderId || Number.isNaN(resolvedFolderId)) {
      throw new Error('A valid folderId is required to upload files.');
    }

    const presigned = await requestPreSignedUrl('upload', {
      folderId: resolvedFolderId,
      fileId,
    });

    const formData = new FormData();
    formData.append('file', file);
    formData.append('folderId', resolvedFolderId);

    const uploadResponse = await axios.post(presigned.data.url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return { data: uploadResponse.data, presigned: presigned.data };
  },
  getByFolder: (folderId) => api.get(`/api/files/folder/${folderId}`),
  getById: (id) => api.get(`/api/files/${id}`),
  delete: (id) => api.delete(`/api/files/${id}`),
  download: async (id) => {
    const presigned = await requestPreSignedUrl('download', { fileId: id });
    const response = await axios.get(presigned.data.url, { responseType: 'blob' });
    return { data: response.data, presigned: presigned.data };
  },
  copySharedFile: (fileId, folderId) =>
    api.post('/api/files/copy', { fileId, folderId }),
  preview: (id) => api.get(`/api/files/${id}/preview`, { responseType: 'blob' }),
};

export const userAPI = {
  getAll: () => api.get('/api/users'),
};

export const permissionAPI = {
  share: (fileId, username, access) =>
    api.post('/api/permissions/share', { fileId, username, access }),
  getFilePermissions: (fileId) =>
    api.get(`/api/permissions/file/${fileId}`),
  getSharedFiles: () =>
    api.get('/api/permissions/shared'),
  getAcceptedSharedFiles: () =>
    api.get('/api/permissions/accepted'),
  getFileOwner: (fileId) =>
    api.get(`/api/permissions/file/${fileId}/owner`),
  markAsViewed: (permissionId) =>
    api.post(`/api/permissions/viewed/${permissionId}`),
  updatePermission: (permissionId, access) =>
    api.put(`/api/permissions/${permissionId}`, { access }),
  revokePermission: (permissionId) =>
    api.delete(`/api/permissions/${permissionId}`),
};

export default api;
