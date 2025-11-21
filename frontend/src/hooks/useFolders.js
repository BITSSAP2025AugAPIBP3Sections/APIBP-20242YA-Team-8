import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { folderAPI } from '../services/api';
import { queryKeys } from '../services/queryClient';

/**
 * Hook to fetch all folders (with caching)
 */
export function useFolders() {
  return useQuery({
    queryKey: queryKeys.folders.lists(),
    queryFn: async () => {
      const response = await folderAPI.getAll();
      // Handle 304 Not Modified response (data is null, fromCache is true)
      if (response.fromCache && !response.data) {
        // Return undefined to use React Query's cached data
        return undefined;
      }
      return response.data;
    },
    // Cache for 10 minutes (folders don't change often)
    staleTime: 10 * 60 * 1000,
    // Keep in cache for 30 minutes (longer for folders since they're accessed frequently)
    gcTime: 30 * 60 * 1000,
  });
}

/**
 * Hook to fetch a single folder (with caching)
 */
export function useFolder(folderId) {
  return useQuery({
    queryKey: queryKeys.folders.detail(folderId),
    queryFn: async () => {
      const response = await folderAPI.getById(folderId);
      return response.data;
    },
    enabled: !!folderId,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook to create a folder (with cache invalidation)
 */
export function useCreateFolder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ name, parentId }) => folderAPI.create(name, parentId),
    onSuccess: (_, variables) => {
      // Invalidate folders list to refetch
      queryClient.invalidateQueries({ queryKey: queryKeys.folders.lists() });
      // Invalidate parent folder if exists
      if (variables.parentId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.folders.detail(variables.parentId) });
      }
    },
  });
}

/**
 * Hook to update a folder (with cache invalidation)
 */
export function useUpdateFolder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, name }) => folderAPI.update(id, name),
    onSuccess: (_, variables) => {
      // Invalidate specific folder and list
      queryClient.invalidateQueries({ queryKey: queryKeys.folders.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.folders.lists() });
    },
  });
}

/**
 * Hook to delete a folder (with cache invalidation)
 */
export function useDeleteFolder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id) => folderAPI.delete(id),
    onSuccess: () => {
      // Invalidate all folder queries
      queryClient.invalidateQueries({ queryKey: queryKeys.folders.all });
      // Also invalidate files since deleting folder affects files
      queryClient.invalidateQueries({ queryKey: queryKeys.files.all });
    },
  });
}

