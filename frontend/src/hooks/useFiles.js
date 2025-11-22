import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fileAPI } from '../services/api';
import { queryKeys } from '../services/queryClient';
import { generateIdempotencyKey, isIdempotent, markIdempotent, getIdempotentResponse } from '../services/idempotency';
import { saveFileForOffline, getOfflineFile, isFileOffline, removeOfflineFile, getAllOfflineFiles } from '../services/offlineStorage';

/**
 * Hook to fetch files in a folder (with caching)
 */
export function useFilesByFolder(folderId) {
  return useQuery({
    queryKey: queryKeys.files.byFolder(folderId),
    queryFn: async () => {
      const response = await fileAPI.getByFolder(folderId);
      return response.data;
    },
    enabled: !!folderId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

/**
 * Hook to download a file (with idempotency and offline support)
 */
export function useDownloadFile() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ fileId, makeOffline = false }) => {
      // Check idempotency
      const idempotencyKey = generateIdempotencyKey('download', { fileId });
      
      if (isIdempotent(idempotencyKey)) {
        const cached = getIdempotentResponse(idempotencyKey);
        if (cached) {
          return cached;
        }
      }
      
      // Check if file is already offline
      if (await isFileOffline(fileId)) {
        const offlineBlob = await getOfflineFile(fileId);
        if (offlineBlob) {
          return { data: offlineBlob, fromCache: true };
        }
      }
      
      // Download from server
      const result = await fileAPI.download(fileId);
      
      // Save for offline if requested
      if (makeOffline) {
        const fileMetadata = await fileAPI.getById(fileId);
        await saveFileForOffline(fileId, result.data, {
          ...fileMetadata.data,
          folderId: fileMetadata.data.folderId,
        });
        // Invalidate offline files query
        queryClient.invalidateQueries({ queryKey: queryKeys.files.offline() });
      }
      
      // Mark as idempotent
      markIdempotent(idempotencyKey, result);
      
      return result;
    },
  });
}

/**
 * Hook to upload a file (with idempotency)
 */
export function useUploadFile() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ file, folderId, fileId, idempotencyKey }) => {
      // Generate idempotency key if not provided
      const key = idempotencyKey || generateIdempotencyKey('upload', { 
        fileName: file.name, 
        fileSize: file.size,
        folderId 
      });
      
      // Check if already processed
      if (isIdempotent(key)) {
        const cached = getIdempotentResponse(key);
        if (cached) {
          return cached;
        }
      }
      
      const result = await fileAPI.upload(file, folderId, fileId);
      
      // Mark as idempotent
      markIdempotent(key, result);
      
      return result;
    },
    onSuccess: (_, variables) => {
      // Invalidate files list for the folder
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.files.byFolder(variables.folderId) 
      });
    },
  });
}

/**
 * Hook to delete a file (with cache invalidation)
 */
export function useDeleteFile() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (fileId) => fileAPI.delete(fileId),
    onSuccess: async (_, fileId) => {
      // Invalidate files queries
      queryClient.invalidateQueries({ queryKey: queryKeys.files.all });
      // Remove from offline storage if exists
      if (await isFileOffline(fileId)) {
        await removeOfflineFile(fileId);
        queryClient.invalidateQueries({ queryKey: queryKeys.files.offline() });
      }
    },
  });
}

/**
 * Hook to check if file is available offline
 */
export function useIsFileOffline(fileId) {
  return useQuery({
    queryKey: ['offline', fileId],
    queryFn: () => isFileOffline(fileId),
    enabled: !!fileId,
  });
}

/**
 * Hook to get all offline files
 */
export function useOfflineFiles() {
  return useQuery({
    queryKey: queryKeys.files.offline(),
    queryFn: async () => {
      return await getAllOfflineFiles();
    },
  });
}

