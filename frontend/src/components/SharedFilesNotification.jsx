import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { permissionAPI } from '../services/api';

const SharedFilesNotification = ({ isOpen, onClose, onFileAccepted }) => {
  const navigate = useNavigate();
  const [sharedFiles, setSharedFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchSharedFiles();
    }
  }, [isOpen]);

  const fetchSharedFiles = async () => {
    try {
      setLoading(true);
      const response = await permissionAPI.getSharedFiles();
      setSharedFiles(response.data || []);
    } catch (err) {
      console.error('Failed to fetch shared files:', err);
      setError('Failed to load shared files');
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptFile = async (file) => {
    if (!file) return;

    setAccepting(true);
    setError('');

    try {
      // Mark permission as viewed (accept the notification)
      await permissionAPI.markAsViewed(file.permissionId);
      
      // Remove from notification list
      setSharedFiles(prev => prev.filter(f => f.permissionId !== file.permissionId));
      
    // Navigate to Shared tab on Folders page
    // navigate('/?tab=shared');
    // setActiveTab('shared');
      
      // Close the notification dialog
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to accept file');
    } finally {
      setAccepting(false);
    }
  };

  const handleRejectFile = async (permissionId) => {
    try {
      // Mark as viewed (dismissed)
      await permissionAPI.markAsViewed(permissionId);
      // Remove from notification list
      setSharedFiles(prev => prev.filter(f => f.permissionId !== permissionId));
      if (sharedFiles.length === 1) {
        onClose();
      }
    } catch (err) {
      console.error('Failed to dismiss notification:', err);
      // Still remove from list even if API call fails
      setSharedFiles(prev => prev.filter(f => f.permissionId !== permissionId));
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b border-gray-200">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-2xl font-bold text-gray-800">Shared Files</h2>
              <p className="text-sm text-gray-600 mt-1">
                Files that have been shared with you
              </p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading ? (
            <div className="text-center py-8">
              <div className="text-gray-500">Loading shared files...</div>
            </div>
          ) : sharedFiles.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-gray-500">No shared files</div>
            </div>
          ) : (
            <div className="space-y-3">
              {sharedFiles.map((file) => (
                <div
                  key={file.permissionId}
                  className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="font-semibold text-gray-800">{file.fileName}</h3>
                        <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                          {file.access}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-1">
                        Size: {formatFileSize(file.fileSize)}
                      </p>
                      <p className="text-xs text-gray-500">
                        Shared by: <span className="font-medium">{file.ownerUsername}</span>
                      </p>
                      <p className="text-xs text-gray-500">
                        From folder: {file.folderName}
                      </p>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <button
                        onClick={() => handleAcceptFile(file)}
                        className="px-3 py-1.5 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700 transition-colors"
                      >
                        Accept
                      </button>
                      <button
                        onClick={() => handleRejectFile(file.permissionId)}
                        className="px-3 py-1.5 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300 transition-colors"
                      >
                        Dismiss
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default SharedFilesNotification;

