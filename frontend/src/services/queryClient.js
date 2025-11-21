import { QueryClient } from '@tanstack/react-query';

/**
 * React Query client configuration
 * Handles caching, stale data, and refetching
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Default: Cache data for 5 minutes
      staleTime: 5 * 60 * 1000,
      // Keep unused data in cache for 10 minutes (garbage collection)
      // This means if you don't use a query for 10 minutes, it's removed from memory
      gcTime: 10 * 60 * 1000,
      // Retry failed requests
      retry: 1,
      // Refetch on window focus (for data consistency) - but only if stale
      refetchOnWindowFocus: true,
      // Don't refetch on mount if data is fresh
      refetchOnMount: false,
    },
    mutations: {
      // Retry mutations once
      retry: 1,
    },
  },
});

/**
 * Query keys for consistent cache management
 */
export const queryKeys = {
  folders: {
    all: ['folders'],
    lists: () => [...queryKeys.folders.all, 'list'],
    detail: (id) => [...queryKeys.folders.all, 'detail', id],
  },
  files: {
    all: ['files'],
    byFolder: (folderId) => [...queryKeys.files.all, 'folder', folderId],
    detail: (id) => [...queryKeys.files.all, 'detail', id],
    offline: () => [...queryKeys.files.all, 'offline'],
  },
  permissions: {
    all: ['permissions'],
    shared: () => [...queryKeys.permissions.all, 'shared'],
    byFile: (fileId) => [...queryKeys.permissions.all, 'file', fileId],
  },
};

