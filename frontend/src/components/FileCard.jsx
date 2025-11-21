import { useState, useEffect } from 'react';
import { permissionAPI } from '../services/api';
import { isFileOffline } from '../services/offlineStorage';

const FileCard = ({ file, onShare, onDownload, onDelete, onMakeOffline, formatFileSize, formatDate }) => {
  const [showOwnerInfo, setShowOwnerInfo] = useState(false);
  const [ownerInfo, setOwnerInfo] = useState(null);
  const [loadingOwner, setLoadingOwner] = useState(false);
  const [isOffline, setIsOffline] = useState(false);
  const [checkingOffline, setCheckingOffline] = useState(true);

  // Check if file is available offline
  useEffect(() => {
    const checkOffline = async () => {
      try {
        const offline = await isFileOffline(file.id);
        setIsOffline(offline);
      } catch (err) {
        console.error('Failed to check offline status:', err);
      } finally {
        setCheckingOffline(false);
      }
    };
    checkOffline();
  }, [file.id]);

  const handleOwnerBadgeClick = async (e) => {
    e.stopPropagation();
    if (!showOwnerInfo && !ownerInfo) {
      setLoadingOwner(true);
      try {
        const response = await permissionAPI.getFileOwner(file.id);
        setOwnerInfo(response.data);
      } catch (err) {
        console.error('Failed to fetch owner info:', err);
        // Fallback to owner info from file if available
        if (file.ownerUsername) {
          setOwnerInfo({ username: file.ownerUsername, id: file.ownerId });
        }
      } finally {
        setLoadingOwner(false);
      }
    }
    setShowOwnerInfo(!showOwnerInfo);
  };
  
  const handleMakeOffline = async (e) => {
    e.stopPropagation();
    if (onMakeOffline) {
      await onMakeOffline(file.id);
      setIsOffline(true);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow relative group">
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-lg font-semibold text-gray-800 truncate">
                {file.originalName}
              </h3>
              {isOffline && (
                <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full flex items-center gap-1" title="Available offline">
                  <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                  </svg>
                  Offline
                </span>
              )}
              {file.isShared && (
                <div className="relative">
                  <button
                    onClick={handleOwnerBadgeClick}
                    className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full hover:bg-blue-200 transition-colors flex items-center gap-1"
                    title="Shared file - Click to see owner"
                  >
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z" />
                    </svg>
                    Shared
                  </button>
                  {showOwnerInfo && ownerInfo && (
                    <div className="absolute top-full left-0 mt-2 z-10 bg-white border border-gray-200 rounded-lg shadow-lg p-3 min-w-[200px]">
                      <div className="text-xs font-semibold text-gray-500 mb-1">Owner</div>
                      <div className="text-sm text-gray-800">{ownerInfo.username}</div>
                      <button
                        onClick={() => setShowOwnerInfo(false)}
                        className="mt-2 text-xs text-indigo-600 hover:text-indigo-700"
                      >
                        Close
                      </button>
                    </div>
                  )}
                  {loadingOwner && (
                    <div className="absolute top-full left-0 mt-2 z-10 bg-white border border-gray-200 rounded-lg shadow-lg p-3 min-w-[200px]">
                      <div className="text-sm text-gray-500">Loading...</div>
                    </div>
                  )}
                </div>
              )}
            </div>
            <p className="text-sm text-gray-500">
              {formatFileSize(file.size)}
            </p>
            <p className="text-xs text-gray-400 mt-1">
              {formatDate(file.uploadedAt)}
            </p>
          </div>
          {!file.isShared && (
            <button
              onClick={onShare}
              className="opacity-0 group-hover:opacity-100 transition-opacity p-2 hover:bg-gray-100 rounded"
              title="Share file"
            >
            <svg
              className="w-5 h-5 text-gray-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            </button>
          )}
        </div>
        <div className="flex items-center justify-between mt-4">
          <div className="flex gap-2">
            <button
              onClick={onDownload}
              className="text-indigo-600 hover:text-indigo-700 text-sm font-medium"
            >
              Download
            </button>
            {onMakeOffline && (
              <button
                onClick={handleMakeOffline}
                disabled={isOffline || checkingOffline}
                className={`text-sm font-medium flex items-center gap-1 ${
                  isOffline 
                    ? 'text-gray-400 cursor-not-allowed' 
                    : 'text-green-600 hover:text-green-700'
                }`}
                title={isOffline ? 'Already available offline' : 'Make available offline'}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {isOffline ? 'Offline ✓' : 'Offline'}
              </button>
            )}
          </div>
          {!file.isShared && (
            <button
              onClick={onDelete}
              className="text-red-600 hover:text-red-700 text-sm"
            >
              Delete
            </button>
          )}
        </div>
      </div>
      {/* Click outside to close owner info */}
      {showOwnerInfo && (
        <div
          className="fixed inset-0 z-0"
          onClick={() => setShowOwnerInfo(false)}
        />
      )}
    </div>
  );
};

export default FileCard;

