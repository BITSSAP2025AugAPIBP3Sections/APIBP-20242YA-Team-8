import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fileAPI, folderAPI } from '../services/api';
import SharingDialog from '../components/SharingDialog';

const Files = () => {
  const { folderId } = useParams();
  const [files, setFiles] = useState([]);
  const [folder, setFolder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);
  const [sharingDialog, setSharingDialog] = useState({ isOpen: false, fileId: null });
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    fetchFolder();
    fetchFiles();
  }, [folderId]);

  const fetchFolder = async () => {
    try {
      const response = await folderAPI.getById(folderId);
      setFolder(response.data);
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
      await fileAPI.upload(file, folderId);
      fetchFiles();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to upload file');
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
      const response = await fileAPI.download(id);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to download file');
    }
  };

  const handleShareClick = (e, fileId) => {
    e.stopPropagation();
    setSharingDialog({ isOpen: true, fileId, folderId });
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
        {/* Error Message */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        {/* Actions */}
        <div className="mb-6 flex justify-between items-center">
          <h2 className="text-3xl font-bold text-gray-800">Files</h2>
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

        {/* Files Grid */}
        {files.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <p className="text-gray-500 text-lg mb-4">No files in this folder</p>
            <label className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 cursor-pointer inline-block">
              Upload Your First File
              <input
                type="file"
                onChange={handleFileUpload}
                className="hidden"
              />
            </label>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {files.map((file) => (
              <div
                key={file.id}
                className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow relative group"
              >
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1 min-w-0">
                      <h3 className="text-lg font-semibold text-gray-800 truncate mb-1">
                        {file.originalName}
                      </h3>
                      <p className="text-sm text-gray-500">
                        {formatFileSize(file.size)}
                      </p>
                      <p className="text-xs text-gray-400 mt-1">
                        {formatDate(file.uploadedAt)}
                      </p>
                    </div>
                    <button
                      onClick={(e) => handleShareClick(e, file.id)}
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
                  </div>
                  <div className="flex items-center justify-between mt-4">
                    <button
                      onClick={() => handleDownload(file.id, file.originalName)}
                      className="text-indigo-600 hover:text-indigo-700 text-sm font-medium"
                    >
                      Download
                    </button>
                    <button
                      onClick={() => handleDeleteFile(file.id)}
                      className="text-red-600 hover:text-red-700 text-sm"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Sharing Dialog */}
      <SharingDialog
        isOpen={sharingDialog.isOpen}
        onClose={() => setSharingDialog({ isOpen: false, fileId: null, folderId: null })}
        folderId={sharingDialog.folderId || folderId}
        files={sharingDialog.fileId ? files.filter((f) => f.id === sharingDialog.fileId) : files}
      />
    </div>
  );
};

export default Files;
