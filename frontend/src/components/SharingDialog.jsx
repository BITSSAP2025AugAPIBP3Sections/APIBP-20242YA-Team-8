import { useState, useEffect } from 'react';
import { userAPI } from '../services/api';

const SharingDialog = ({ isOpen, onClose, folderId, files = [] }) => {
  const [users, setUsers] = useState([]);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchUsers();
    }
  }, [isOpen]);

  const fetchUsers = async () => {
    try {
      // Note: This endpoint needs to be created in the backend
      // For now, we'll handle the error gracefully
      const response = await userAPI.getAll();
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('Unable to fetch users. The backend endpoint /api/users may not exist yet.');
      // You could show a mock user list here for demo purposes
    }
  };

  const handleUserToggle = (userId) => {
    setSelectedUsers((prev) =>
      prev.includes(userId)
        ? prev.filter((id) => id !== userId)
        : [...prev, userId]
    );
  };

  const handleFileToggle = (fileId) => {
    setSelectedFiles((prev) =>
      prev.includes(fileId)
        ? prev.filter((id) => id !== fileId)
        : [...prev, fileId]
    );
  };

  const handleShare = async () => {
    if (selectedUsers.length === 0) {
      setError('Please select at least one user');
      return;
    }

    setLoading(true);
    setError('');

    try {
      // TODO: Implement sharing endpoint in backend
      // await shareAPI.share({ folderId, fileIds: selectedFiles, userIds: selectedUsers });
      alert(
        `Sharing functionality would be implemented here.\n` +
        `Folder ID: ${folderId}\n` +
        `Selected Files: ${selectedFiles.join(', ') || 'All files in folder'}\n` +
        `Selected Users: ${selectedUsers.join(', ')}`
      );
      onClose();
      // Reset state
      setSelectedUsers([]);
      setSelectedFiles([]);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to share');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-800">Share Files</h2>
          <p className="text-sm text-gray-600 mt-1">
            Select users and files to share from this folder
          </p>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {/* File Selection */}
          <div>
            <h3 className="text-lg font-semibold text-gray-700 mb-3">
              Select Files to Share
            </h3>
            <div className="space-y-2 max-h-40 overflow-y-auto border border-gray-200 rounded p-3">
              {files.length === 0 ? (
                <p className="text-gray-500 text-sm">No files in this folder</p>
              ) : (
                <>
                  <label className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded">
                    <input
                      type="checkbox"
                      checked={selectedFiles.length === files.length}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedFiles(files.map((f) => f.id));
                        } else {
                          setSelectedFiles([]);
                        }
                      }}
                      className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span className="text-sm font-medium">Select All Files</span>
                  </label>
                  {files.map((file) => (
                    <label
                      key={file.id}
                      className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
                    >
                      <input
                        type="checkbox"
                        checked={selectedFiles.includes(file.id)}
                        onChange={() => handleFileToggle(file.id)}
                        className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="text-sm text-gray-700">{file.originalName}</span>
                      <span className="text-xs text-gray-500">
                        ({(file.size / 1024).toFixed(2)} KB)
                      </span>
                    </label>
                  ))}
                </>
              )}
            </div>
          </div>

          {/* User Selection */}
          <div>
            <h3 className="text-lg font-semibold text-gray-700 mb-3">
              Select Users to Share With
            </h3>
            <div className="space-y-2 max-h-60 overflow-y-auto border border-gray-200 rounded p-3">
              {users.length === 0 ? (
                <p className="text-gray-500 text-sm">
                  {error
                    ? 'Unable to load users. Please ensure the /api/users endpoint exists.'
                    : 'Loading users...'}
                </p>
              ) : (
                users.map((user) => (
                  <label
                    key={user.id}
                    className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
                  >
                    <input
                      type="checkbox"
                      checked={selectedUsers.includes(user.id)}
                      onChange={() => handleUserToggle(user.id)}
                      className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span className="text-sm text-gray-700">{user.username}</span>
                  </label>
                ))
              )}
            </div>
          </div>
        </div>

        <div className="p-6 border-t border-gray-200 flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleShare}
            disabled={loading || selectedUsers.length === 0}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? 'Sharing...' : 'Share'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SharingDialog;
