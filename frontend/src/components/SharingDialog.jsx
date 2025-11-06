import { useState, useEffect } from 'react';
import { userAPI, permissionAPI, fileAPI } from '../services/api';

const SharingDialog = ({ isOpen, onClose, folderId, fileId, files = [] }) => {
  const [users, setUsers] = useState([]);
  const [folderFiles, setFolderFiles] = useState([]);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [accessLevel, setAccessLevel] = useState('READ');
  const [loading, setLoading] = useState(false);
  const [loadingFiles, setLoadingFiles] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [existingPermissions, setExistingPermissions] = useState({}); // {fileId: [{id, username, access}]}
  const [fileOwner, setFileOwner] = useState(null); // Owner username for the file
  const [updatingPermissions, setUpdatingPermissions] = useState({}); // {permissionId: true}
  const [revokingPermissions, setRevokingPermissions] = useState({}); // {permissionId: true}
  const [activeInfoFileId, setActiveInfoFileId] = useState(null); // The fileId whose permissions are shown below

  // Use files prop if provided, otherwise use folderFiles
  const displayFiles = files.length > 0 ? files : folderFiles;

  useEffect(() => {
    if (isOpen) {
      fetchUsers();
      // Reset state when dialog opens
      setSelectedUsers([]);
      setSelectedFiles([]);
      setAccessLevel('READ');
      setError('');
      setSuccess('');
      setActiveInfoFileId(null);
      
      // If a specific fileId is provided, select it
      if (fileId) {
        setSelectedFiles([fileId]);
        setActiveInfoFileId(fileId);
        fetchFilePermissions(fileId);
      }
      
      // If folderId is provided but no files, fetch files from folder
      if (folderId && files.length === 0) {
        fetchFilesFromFolder(folderId);
      }
    }
  }, [isOpen, fileId, folderId]);

  // When selection changes at folder-level, show permissions for a single selected file
  useEffect(() => {
    if (!fileId) {
      if (selectedFiles.length === 1) {
        const fId = selectedFiles[0];
        setActiveInfoFileId(fId);
        fetchFilePermissions(fId);
      } else {
        setActiveInfoFileId(null);
      }
    }
  }, [selectedFiles, fileId]);

  const fetchFilePermissions = async (fileId) => {
    try {
      const response = await permissionAPI.getFilePermissions(fileId);
      const permissions = response.data || [];
      
      // Find owner
      const owner = permissions.find(p => p.access === 'OWNER');
      if (owner) {
        setFileOwner(owner.username);
      }
      
      // Filter out OWNER permissions and group by fileId
      const filePerms = permissions
        .filter(p => p.access !== 'OWNER')
        .map(p => ({ id: p.id, username: p.username, access: p.access }));
      setExistingPermissions(prev => ({ ...prev, [fileId]: filePerms }));
    } catch (err) {
      console.error('Failed to fetch file permissions:', err);
    }
  };

  const fetchFilesFromFolder = async (folderId) => {
    try {
      setLoadingFiles(true);
      const response = await fileAPI.getByFolder(folderId);
      setFolderFiles(response.data);
    } catch (err) {
      console.error('Failed to fetch files from folder:', err);
      setError('Unable to fetch files from folder. Only files can be shared, not folders.');
    } finally {
      setLoadingFiles(false);
    }
  };

  const fetchUsers = async () => {
    try {
      const response = await userAPI.getAll();
      setUsers(response.data);
      setError('');
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('Unable to fetch users. Please try again.');
    }
  };

  const handleUserToggle = (username) => {
    setSelectedUsers((prev) =>
      prev.includes(username)
        ? prev.filter((u) => u !== username)
        : [...prev, username]
    );
  };

  const handleFileToggle = (fileId) => {
    setSelectedFiles((prev) =>
      prev.includes(fileId)
        ? prev.filter((id) => id !== fileId)
        : [...prev, fileId]
    );
  };

  const handleUpdatePermission = async (permissionId, newAccess) => {
    setUpdatingPermissions(prev => ({ ...prev, [permissionId]: true }));
    try {
      await permissionAPI.updatePermission(permissionId, newAccess);
      setSuccess('Permission updated successfully');
      // Refresh permissions
      if (activeInfoFileId) {
        await fetchFilePermissions(activeInfoFileId);
      }
      // Refresh after a delay to show success message
      setTimeout(() => {
        setSuccess('');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update permission');
    } finally {
      setUpdatingPermissions(prev => ({ ...prev, [permissionId]: false }));
    }
  };

  const handleRevokePermission = async (permissionId) => {
    if (!window.confirm('Are you sure you want to revoke this user\'s access? They will be notified.')) {
      return;
    }
    
    setRevokingPermissions(prev => ({ ...prev, [permissionId]: true }));
    try {
      await permissionAPI.revokePermission(permissionId);
      setSuccess('Permission revoked successfully');
      // Refresh permissions
      if (activeInfoFileId) {
        await fetchFilePermissions(activeInfoFileId);
      }
      // Refresh after a delay to show success message
      setTimeout(() => {
        setSuccess('');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to revoke permission');
    } finally {
      setRevokingPermissions(prev => ({ ...prev, [permissionId]: false }));
    }
  };

  const handleShare = async () => {
    if (selectedUsers.length === 0) {
      setError('Please select at least one user');
      return;
    }

    // Determine which files to share
    const filesToShare = selectedFiles.length > 0 ? selectedFiles : displayFiles.map(f => f.id);
    
    if (filesToShare.length === 0) {
      setError('Please select at least one file to share');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      // Share each file with each selected user
      const sharePromises = [];
      for (const fileId of filesToShare) {
        for (const username of selectedUsers) {
          sharePromises.push(
            permissionAPI.share(fileId.toString(), username, accessLevel)
          );
        }
      }

      await Promise.all(sharePromises);
      setSuccess(`Successfully shared ${filesToShare.length} file(s) with ${selectedUsers.length} user(s)`);
      
      // Close dialog after a short delay to show success message
      setTimeout(() => {
        onClose();
        setSelectedUsers([]);
        setSelectedFiles([]);
        setSuccess('');
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to share files. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w_full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-800">Share Files</h2>
          <p className="text-sm text_gray-600 mt-1">
            Select users and files to share from this folder
          </p>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
          
          {success && (
            <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded">
              {success}
            </div>
          )}

          {/* File Selection */}
          {loadingFiles ? (
            <div className="text-center py-4">
              <p className="text-gray-500 text-sm">Loading files...</p>
            </div>
          ) : displayFiles.length > 0 ? (
            <div>
              <h3 className="text-lg font-semibold text-gray-700 mb-3">
                Select Files to Share
              </h3>
              <div className="space-y-2 max-h-40 overflow-y-auto border border-gray-200 rounded p-3">
                <label className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded">
                  <input
                    type="checkbox"
                    checked={selectedFiles.length === displayFiles.length && displayFiles.length > 0}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedFiles(displayFiles.map((f) => f.id));
                      } else {
                        setSelectedFiles([]);
                      }
                    }}
                    className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span className="text-sm font-medium">Select All Files</span>
                </label>
                {displayFiles.map((file) => (
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
              </div>
            </div>
          ) : folderId && !loadingFiles ? (
            <div className="bg-yellow-50 border border-yellow-200 rounded p-4">
              <p className="text-yellow-800 text-sm">
                This folder is empty. Only files can be shared, not folders.
              </p>
            </div>
          ) : null}

          {/* Access Level Selection */}
          <div>
            <h3 className="text-lg font-semibold text-gray-700 mb-3">
              Access Level
            </h3>
            <div className="space-y-2 border border-gray-200 rounded p-3">
              <label className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded">
                <input
                  type="radio"
                  name="access"
                  value="READ"
                  checked={accessLevel === 'READ'}
                  onChange={(e) => setAccessLevel(e.target.value)}
                  className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-700">Read Only</span>
                  <p className="text-xs text-gray-500">Users can view and download the file</p>
                </div>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded">
                <input
                  type="radio"
                  name="access"
                  value="WRITE"
                  checked={accessLevel === 'WRITE'}
                  onChange={(e) => setAccessLevel(e.target.value)}
                  className="w-4 h-4 text-indigo-600 focus:ring-indigo-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-700">Read & Write</span>
                  <p className="text-xs text-gray-500">Users can view, download, and modify the file</p>
                </div>
              </label>
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
                    ? 'Unable to load users. Please try again.'
                    : 'Loading users...'}
                </p>
              ) : (
                users.map((user) => {
                  // Check if user is the owner (disable owner)
                  const isOwner = fileOwner && user.username === fileOwner;
                  
                  // Check if user is already shared for any selected file
                  const isAlreadyShared = selectedFiles.length > 0 && 
                    selectedFiles.some(fId => {
                      const perms = existingPermissions[fId] || [];
                      return perms.some(p => p.username === user.username);
                    });
                  
                  // Get existing access level if shared
                  const existingAccess = selectedFiles.length > 0 ? 
                    selectedFiles.map(fId => existingPermissions[fId] || [])
                      .flat()
                      .find(p => p.username === user.username)?.access : null;

                  const isDisabled = isOwner || isAlreadyShared;

                  return (
                    <label
                      key={user.id}
                      className={`flex items-center space-x-2 p-2 rounded ${
                        isDisabled 
                          ? 'bg-gray-100 opacity-75 cursor-not-allowed' 
                          : 'cursor-pointer hover:bg-gray-50'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={selectedUsers.includes(user.username)}
                        onChange={() => handleUserToggle(user.username)}
                        disabled={isDisabled}
                        className="w-4 h-4 text-indigo-600 focus:ring-indigo-500 disabled:cursor-not-allowed"
                      />
                      <span className="text-sm text-gray-700 flex-1">{user.username}</span>
                      {isOwner && (
                        <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-1 rounded">
                          Owner
                        </span>
                      )}
                      {isAlreadyShared && existingAccess && !isOwner && (
                        <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded">
                          Already shared ({existingAccess})
                        </span>
                      )}
                    </label>
                  );
                })
              )}
            </div>
          </div>
          
          {/* Show existing shares info with management */}
          {activeInfoFileId && existingPermissions[activeInfoFileId] && existingPermissions[activeInfoFileId].length > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded p-4">
              <h4 className="text-sm font-semibold text-blue-900 mb-3">
                Currently Shared With:
              </h4>
              <div className="space-y-2">
                {existingPermissions[activeInfoFileId].map((perm) => (
                  <div key={perm.id} className="flex items-center justify-between bg-white rounded p-2">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-800">{perm.username}</span>
                      <select
                        value={perm.access}
                        onChange={(e) => handleUpdatePermission(perm.id, e.target.value)}
                        disabled={updatingPermissions[perm.id]}
                        className="text-xs border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
                      >
                        <option value="READ">READ</option>
                        <option value="WRITE">WRITE</option>
                      </select>
                    </div>
                    <button
                      onClick={() => handleRevokePermission(perm.id)}
                      disabled={revokingPermissions[perm.id]}
                      className="text-xs text-red-600 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors disabled:opacity-50"
                      title="Revoke access"
                    >
                      {revokingPermissions[perm.id] ? 'Revoking...' : 'Revoke'}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
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
            disabled={loading || selectedUsers.length === 0 || (displayFiles.length > 0 && selectedFiles.length === 0 && !fileId) || loadingFiles}
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
