import { openDB } from 'idb';

const DB_NAME = 'VaultifyOffline';
const DB_VERSION = 2; // Updated to match existing database version
const STORE_FILES = 'offlineFiles';
const STORE_METADATA = 'fileMetadata';

/**
 * Initialize IndexedDB for offline file storage
 * Handles version upgrades gracefully
 */
async function initDB() {
  try {
    return await openDB(DB_NAME, DB_VERSION, {
      upgrade(db, oldVersion, newVersion, transaction) {
        // Handle upgrades from version 1 to 2
        if (oldVersion < 2) {
          // Store for offline file content (blobs)
          if (!db.objectStoreNames.contains(STORE_FILES)) {
            const fileStore = db.createObjectStore(STORE_FILES, { keyPath: 'fileId' });
            fileStore.createIndex('folderId', 'folderId');
            fileStore.createIndex('cachedAt', 'cachedAt');
          }
          
          // Store for file metadata
          if (!db.objectStoreNames.contains(STORE_METADATA)) {
            const metadataStore = db.createObjectStore(STORE_METADATA, { keyPath: 'fileId' });
            metadataStore.createIndex('folderId', 'folderId');
            metadataStore.createIndex('cachedAt', 'cachedAt');
          }
        }
      },
      blocked() {
        console.warn('IndexedDB is blocked. Please close other tabs using this database.');
      },
      blocking() {
        console.warn('IndexedDB version upgrade is blocked. Please refresh the page.');
      },
    });
  } catch (error) {
    // If version error, try to delete and recreate
    if (error.name === 'VersionError') {
      console.warn('IndexedDB version mismatch. Attempting to reset...');
      try {
        // Try to delete the existing database
        const deleteReq = indexedDB.deleteDatabase(DB_NAME);
        await new Promise((resolve, reject) => {
          deleteReq.onsuccess = () => resolve();
          deleteReq.onerror = () => reject(deleteReq.error);
          deleteReq.onblocked = () => {
            console.warn('Cannot delete database - it is in use');
            resolve(); // Continue anyway
          };
        });
        // Retry opening with new version
        return await openDB(DB_NAME, DB_VERSION, {
          upgrade(db) {
            const fileStore = db.createObjectStore(STORE_FILES, { keyPath: 'fileId' });
            fileStore.createIndex('folderId', 'folderId');
            fileStore.createIndex('cachedAt', 'cachedAt');
            
            const metadataStore = db.createObjectStore(STORE_METADATA, { keyPath: 'fileId' });
            metadataStore.createIndex('folderId', 'folderId');
            metadataStore.createIndex('cachedAt', 'cachedAt');
          },
        });
      } catch (resetError) {
        console.error('Failed to reset IndexedDB:', resetError);
        throw resetError;
      }
    }
    throw error;
  }
}

/**
 * Save file content for offline access
 */
export async function saveFileForOffline(fileId, fileBlob, metadata) {
  const db = await initDB();
  const tx = db.transaction(STORE_FILES, 'readwrite');
  
  await tx.store.put({
    fileId,
    blob: fileBlob,
    folderId: metadata.folderId,
    originalName: metadata.originalName,
    contentType: metadata.contentType,
    size: metadata.size,
    cachedAt: Date.now(),
  });
  
  // Also save metadata separately for quick access
  const metadataTx = db.transaction(STORE_METADATA, 'readwrite');
  await metadataTx.store.put({
    fileId,
    ...metadata,
    cachedAt: Date.now(),
    isOffline: true,
  });
  
  await tx.done;
  await metadataTx.done;
}

/**
 * Get offline file content
 */
export async function getOfflineFile(fileId) {
  const db = await initDB();
  const file = await db.get(STORE_FILES, fileId);
  return file ? file.blob : null;
}

/**
 * Get offline file metadata
 */
export async function getOfflineFileMetadata(fileId) {
  const db = await initDB();
  return await db.get(STORE_METADATA, fileId);
}

/**
 * Get all offline files
 */
export async function getAllOfflineFiles() {
  const db = await initDB();
  return await db.getAll(STORE_METADATA);
}

/**
 * Get offline files for a specific folder
 */
export async function getOfflineFilesByFolder(folderId) {
  const db = await initDB();
  const index = db.transaction(STORE_METADATA).store.index('folderId');
  return await index.getAll(folderId);
}

/**
 * Remove file from offline storage
 */
export async function removeOfflineFile(fileId) {
  const db = await initDB();
  const tx = db.transaction([STORE_FILES, STORE_METADATA], 'readwrite');
  
  await tx.objectStore(STORE_FILES).delete(fileId);
  await tx.objectStore(STORE_METADATA).delete(fileId);
  
  await tx.done;
}

/**
 * Check if file is available offline
 */
export async function isFileOffline(fileId) {
  const db = await initDB();
  const metadata = await db.get(STORE_METADATA, fileId);
  return !!metadata;
}

/**
 * Get total offline storage size (approximate)
 */
export async function getOfflineStorageSize() {
  const db = await initDB();
  const files = await db.getAll(STORE_FILES);
  return files.reduce((total, file) => total + (file.size || 0), 0);
}

/**
 * Clear all offline files
 */
export async function clearAllOfflineFiles() {
  const db = await initDB();
  const tx = db.transaction([STORE_FILES, STORE_METADATA], 'readwrite');
  
  await tx.objectStore(STORE_FILES).clear();
  await tx.objectStore(STORE_METADATA).clear();
  
  await tx.done;
}

