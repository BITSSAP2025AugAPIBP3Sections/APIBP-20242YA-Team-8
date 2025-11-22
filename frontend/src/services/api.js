import axios from 'axios';
import { generateIdempotencyKey } from './idempotency';
import { queryClient } from './queryClient';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Store ETags for cache validation
const etagCache = new Map();

// Expose etagCache to window for clearing on logout/login
if (typeof window !== 'undefined') {
  window.etagCache = etagCache;
}

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

// Handle unauthorized and rate limit responses
api.interceptors.response.use(
  (response) => {
    // Store rate limit headers for potential use
    if (response.headers['x-ratelimit-remaining']) {
      // Optionally log or store rate limit info
      console.debug('Rate limit remaining:', response.headers['x-ratelimit-remaining']);
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Clear all caches on 401 (unauthorized)
      queryClient.clear();
      etagCache.clear();
      localStorage.removeItem('token');
      window.location.href = '/login';
    } else if (error.response?.status === 429) {
      // Rate limit exceeded
      const retryAfter = error.response.headers['retry-after'] || 
                        error.response.data?.retryAfter || 
                        60;
      const message = error.response.data?.message || 
                     error.response.data?.error || 
                     'Too many requests. Please try again later.';
      
      // Create a more detailed error message
      error.rateLimitInfo = {
        message,
        retryAfter: parseInt(retryAfter),
        resetTime: error.response.headers['x-ratelimit-reset'] 
          ? new Date(parseInt(error.response.headers['x-ratelimit-reset']) * 1000)
          : new Date(Date.now() + retryAfter * 1000),
        limit: error.response.headers['x-ratelimit-limit'],
        remaining: error.response.headers['x-ratelimit-remaining'],
        type: error.response.headers['x-ratelimit-type'] || 'api'
      };
      
      // Log rate limit info for debugging
      console.warn('Rate limit exceeded:', error.rateLimitInfo);
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
  getAll: async () => {
    const url = '/api/folders';
    const etag = etagCache.get(url);
    const headers = {};
    if (etag) {
      headers['If-None-Match'] = etag;
    }
    
    try {
      const response = await api.get(url, { headers });
      if (response.headers.etag) {
        etagCache.set(url, response.headers.etag);
      }
      return response;
    } catch (error) {
      if (error.response?.status === 304) {
        return { data: null, fromCache: true, etag: etag };
      }
      throw error;
    }
  },
  getById: async (id) => {
    const url = `/api/folders/${id}`;
    const etag = etagCache.get(url);
    const headers = {};
    if (etag) {
      headers['If-None-Match'] = etag;
    }
    
    try {
      const response = await api.get(url, { headers });
      if (response.headers.etag) {
        etagCache.set(url, response.headers.etag);
      }
      return response;
    } catch (error) {
      if (error.response?.status === 304) {
        return { data: null, fromCache: true, etag: etag };
      }
      throw error;
    }
  },
  create: (name, parentId) => {
    // Invalidate cache
    etagCache.delete('/api/folders');
    if (parentId) {
      etagCache.delete(`/api/folders/${parentId}`);
    }
    return api.post('/api/folders', { name, parentId: parentId || null });
  },
  update: (id, name) => {
    // Invalidate cache
    etagCache.delete('/api/folders');
    etagCache.delete(`/api/folders/${id}`);
    return api.put(`/api/folders/${id}`, { name });
  },
  delete: (id) => {
    // Invalidate cache
    etagCache.delete('/api/folders');
    etagCache.delete(`/api/folders/${id}`);
    return api.delete(`/api/folders/${id}`);
  },
};

const PRESIGN_BASE_PATH = '/api/v1/files/presign';

const requestPreSignedUrl = (path, payload) =>
  api.post(`${PRESIGN_BASE_PATH}/${path}`, payload);

export const fileAPI = {
  upload: async (file, folderId, fileId = null, idempotencyKey = null) => {
    const resolvedFolderId =
      folderId !== undefined && folderId !== null ? Number(folderId) : null;

    if (!resolvedFolderId || Number.isNaN(resolvedFolderId)) {
      throw new Error('A valid folderId is required to upload files.');
    }

    const key = idempotencyKey || generateIdempotencyKey('upload', {
      fileName: file.name,
      fileSize: file.size,
      folderId: resolvedFolderId,
    });

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
        'Idempotency-Key': key,
      },
    });

    return { data: uploadResponse.data, presigned: presigned.data };
  },
  getByFolder: async (folderId) => {
    const url = `/api/files/folder/${folderId}`;
    const etag = etagCache.get(url);
    const headers = {};
    if (etag) {
      headers['If-None-Match'] = etag;
    }
    
    try {
      const response = await api.get(url, { headers });
      // Store ETag from response
      if (response.headers.etag) {
        etagCache.set(url, response.headers.etag);
      }
      return response;
    } catch (error) {
      // 304 Not Modified - return cached data
      if (error.response?.status === 304) {
        return { data: null, fromCache: true, etag: etag };
      }
      throw error;
    }
  },
  getById: async (id) => {
    const url = `/api/files/${id}`;
    const etag = etagCache.get(url);
    const headers = {};
    if (etag) {
      headers['If-None-Match'] = etag;
    }
    
    try {
      const response = await api.get(url, { headers });
      if (response.headers.etag) {
        etagCache.set(url, response.headers.etag);
      }
      return response;
    } catch (error) {
      if (error.response?.status === 304) {
        return { data: null, fromCache: true, etag: etag };
      }
      throw error;
    }
  },
  delete: (id) => {
    // Invalidate ETag cache
    etagCache.delete(`/api/files/${id}`);
    etagCache.delete(`/api/files/folder/${id}`);
    return api.delete(`/api/files/${id}`);
  },
  download: async (id, idempotencyKey = null) => {
    const key = idempotencyKey || generateIdempotencyKey('download', { fileId: id });
    const presigned = await requestPreSignedUrl('download', { fileId: id });
    
    const response = await axios.get(presigned.data.url, {
      responseType: 'blob',
      headers: {
        'Idempotency-Key': key,
      },
    });
    
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
