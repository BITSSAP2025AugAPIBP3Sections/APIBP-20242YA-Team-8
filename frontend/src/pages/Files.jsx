import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fileAPI, folderAPI } from '../services/api';
import SharingDialog from '../components/SharingDialog';
import FileCard from '../components/FileCard';

const Files = () => {
  const { folderId } = useParams();
  const [files, setFiles] = useState([]);
  const [subfolders, setSubfolders] = useState([]);
  const [folder, setFolder] = useState(null);
  const [breadcrumbPath, setBreadcrumbPath] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [uploading, setUploading] = useState(false);
  const [showCreateSubfolderDialog, setShowCreateSubfolderDialog] = useState(false);
  const [newSubfolderName, setNewSubfolderName] = useState('');
  const [sharingDialog, setSharingDialog] = useState({ isOpen: false, fileId: null, folderId: null });
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    fetchFolder();
    fetchFiles();
  }, [folderId]);

  // Listen for file acceptance events (from SharedFilesNotification)
  useEffect(() => {
    const handleFileAccepted = (event) => {
      // Refresh files when a file is accepted in the current folder
      if (folderId && event.detail && event.detail.folderId === Number(folderId)) {
        fetchFiles();
      }
    };
    
    window.addEventListener('fileAccepted', handleFileAccepted);
    return () => window.removeEventListener('fileAccepted', handleFileAccepted);
  }, [folderId]);

  const buildBreadcrumbPath = async (currentFolderId) => {
    const path = [];
    let currentId = currentFolderId;

    while (currentId) {
      try {
        const response = await folderAPI.getById(currentId);
        const folderData = response.data;
        path.unshift({ id: folderData.id, name: folderData.name });
        
        if (folderData.parent) {
          currentId = folderData.parent.id;
        } else {
          currentId = null;
        }
      } catch (err) {
        console.error('Error building breadcrumb:', err);
        break;
      }
    }

    // Add "Home" or "Folders" at the beginning
    path.unshift({ id: null, name: 'Folders' });
    setBreadcrumbPath(path);
  };

  const fetchFolder = async () => {
    try {
      const response = await folderAPI.getById(folderId);
      setFolder(response.data);
      // Extract subfolders from children
      if (response.data.children) {
        setSubfolders(response.data.children);
      } else {
        setSubfolders([]);
      }
      // Build breadcrumb path
      await buildBreadcrumbPath(folderId);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch folder');
    }
  };

  const fetchFiles = async () => {
    try {
      setLoading(true);
      const response = await fileAPI.getByFolder(folderId);
      setFiles(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch files');
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      setUploading(true);
      const { presigned } = await fileAPI.upload(file, folderId);
      setInfoMessage(
        `Upload completed via pre-signed link (initial TTL: ${presigned.expiresInSeconds}s)`
      );
      setError('');
      fetchFiles();
    } catch (err) {
      setInfoMessage('');
      const message = err.response?.data?.error || err.message || 'Failed to upload file';
      setError(message);
    } finally {
      setUploading(false);
      e.target.value = ''; // Reset input
    }
  };

  const handleDeleteFile = async (id) => {
    if (!window.confirm('Are you sure you want to delete this file?')) return;

    try {
      await fileAPI.delete(id);
      fetchFiles();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete file');
    }
  };

  const handleDownload = async (id, filename) => {
    try {
      const { data, presigned } = await fileAPI.download(id);
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setInfoMessage(
        `Pre-signed download token valid for ${presigned.expiresInSeconds}s (auto-invalidated after use).`
      );
      setError('');
    } catch (err) {
      setInfoMessage('');
      setError(err.response?.data?.error || 'Failed to download file');
    }
  };

  const handleShareClick = (e, fileId) => {
    e.stopPropagation();
    setSharingDialog({ isOpen: true, fileId, folderId });
  };

  const handleCreateSubfolder = async (e) => {
    e.preventDefault();
    if (!newSubfolderName.trim()) return;

    try {
      await folderAPI.create(newSubfolderName.trim(), folderId);
      setNewSubfolderName('');
      setShowCreateSubfolderDialog(false);
      fetchFolder(); // Refresh to get new subfolder
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create subfolder');
    }
  };

  const handleSubfolderClick = (subfolderId) => {
    navigate(`/folders/${subfolderId}/files`);
  };

  const handleDeleteSubfolder = async (id) => {
    if (!window.confirm('Are you sure you want to delete this subfolder?')) return;

    try {
      await folderAPI.delete(id);
      fetchFolder(); // Refresh to remove deleted subfolder
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete subfolder');
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading files...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <button
                onClick={() => navigate('/folders')}
                className="text-indigo-600 hover:text-indigo-700 mb-2 flex items-center"
              >
                <svg
                  className="w-5 h-5 mr-1"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 19l-7-7 7-7"
                  />
                </svg>
                Back to Folders
              </button>
              <h1 className="text-2xl font-bold text-gray-800">
                {folder?.name || 'Files'}
              </h1>
              <p className="text-sm text-gray-600">Welcome, {user?.username}</p>
            </div>
            <button
              onClick={logout}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Status Messages */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 flex justify-between items-center">
            <span>{error}</span>
            <button
              onClick={() => setError('')}
              className="text-red-700 hover:text-red-900 font-semibold"
            >
              Dismiss
            </button>
          </div>
        )}
        {infoMessage && (
          <div className="bg-blue-50 border border-blue-200 text-blue-800 px-4 py-3 rounded mb-4 flex justify-between items-center">
            <span>{infoMessage}</span>
            <button
              onClick={() => setInfoMessage('')}
              className="text-blue-700 hover:text-blue-900 font-semibold"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Breadcrumb Navigation */}
        {breadcrumbPath.length > 0 && (
          <nav className="mb-6 bg-white rounded-lg shadow-sm px-4 py-3 flex items-center space-x-2 text-sm border border-gray-200">
            {breadcrumbPath.map((item, index) => (
              <div key={item.id || 'home'} className="flex items-center space-x-2">
                {index > 0 && (
                  <svg
                    className="w-4 h-4 text-gray-400 mx-1"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                )}
                {index === breadcrumbPath.length - 1 ? (
                  <span className="text-gray-900 font-semibold">{item.name}</span>
                ) : (
                  <button
                    onClick={() => {
                      if (item.id === null) {
                        navigate('/folders');
                      } else {
                        navigate(`/folders/${item.id}/files`);
                      }
                    }}
                    className="text-indigo-600 hover:text-indigo-700 hover:underline transition-colors"
                  >
                    {item.name}
                  </button>
                )}
              </div>
            ))}
          </nav>
        )}

        {/* Actions */}
        <div className="mb-6 flex justify-between items-center">
          <h2 className="text-3xl font-bold text-gray-800">Files & Folders</h2>
          <div className="flex gap-3">
            <button
              onClick={() => setShowCreateSubfolderDialog(true)}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
            >
              + Create Subfolder
            </button>
            <label className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors cursor-pointer inline-block">
              {uploading ? 'Uploading...' : '+ Upload File'}
              <input
                type="file"
                onChange={handleFileUpload}
                disabled={uploading}
                className="hidden"
              />
            </label>
          </div>
        </div>

        {/* Subfolders and Files Grid */}
        {subfolders.length === 0 && files.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <p className="text-gray-500 text-lg mb-4">This folder is empty</p>
            <div className="flex gap-3 justify-center">
              <button
                onClick={() => setShowCreateSubfolderDialog(true)}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
              >
                Create Subfolder
              </button>
              <label className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 cursor-pointer inline-block">
                Upload File
                <input
                  type="file"
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {/* Subfolders */}
            {subfolders.map((subfolder) => (
              <div
                key={subfolder.id}
                className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow relative group cursor-pointer"
                onClick={() => handleSubfolderClick(subfolder.id)}
              >
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2">
                        <svg
                          className="w-6 h-6 text-yellow-500"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                          />
                        </svg>
                        <h3 className="text-lg font-semibold text-gray-800 truncate">
                          {subfolder.name}
                        </h3>
                      </div>
                      <p className="text-sm text-gray-500">Folder</p>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSharingDialog({ isOpen: true, fileId: null, folderId: subfolder.id });
                      }}
                      className="opacity-0 group-hover:opacity-100 transition-opacity p-2 hover:bg-gray-100 rounded"
                      title="Share folder"
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
                  </div>
                  <div className="flex items-center justify-between mt-4">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSubfolderClick(subfolder.id);
                      }}
                      className="text-indigo-600 hover:text-indigo-700 text-sm font-medium"
                    >
                      Open
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteSubfolder(subfolder.id);
                      }}
                      className="text-red-600 hover:text-red-700 text-sm"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}

            {/* Files */}
            {files.map((file) => (
              <FileCard
                key={file.id}
                file={file}
                onShare={(e) => handleShareClick(e, file.id)}
                onDownload={() => handleDownload(file.id, file.originalName)}
                onDelete={() => handleDeleteFile(file.id)}
                formatFileSize={formatFileSize}
                formatDate={formatDate}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create Subfolder Dialog */}
      {showCreateSubfolderDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Create Subfolder</h3>
            <form onSubmit={handleCreateSubfolder}>
              <input
                type="text"
                value={newSubfolderName}
                onChange={(e) => setNewSubfolderName(e.target.value)}
                placeholder="Subfolder name"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent mb-4"
                autoFocus
              />
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateSubfolderDialog(false);
                    setNewSubfolderName('');
                  }}
                  className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Sharing Dialog */}
      <SharingDialog
        isOpen={sharingDialog.isOpen}
        onClose={() => setSharingDialog({ isOpen: false, fileId: null, folderId: null })}
        folderId={sharingDialog.folderId || folderId}
        fileId={sharingDialog.fileId}
        files={files}
      />
    </div>
  );
};

export default Files;
