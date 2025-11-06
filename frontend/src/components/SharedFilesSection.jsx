import { useState, useEffect } from 'react';
import { permissionAPI, fileAPI, folderAPI } from '../services/api';

const SharedFilesSection = () => {
  const [sharedFiles, setSharedFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [previewFile, setPreviewFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [showMoveDialog, setShowMoveDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [folders, setFolders] = useState([]);
  const [targetFolderId, setTargetFolderId] = useState(null);
  const [moving, setMoving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchSharedFiles();
  }, []);

  const fetchSharedFiles = async () => {
    try {
      setLoading(true);
      const response = await permissionAPI.getAcceptedSharedFiles();
      setSharedFiles(response.data || []);
    } catch (err) {
      console.error('Failed to fetch shared files:', err);
      setError('Failed to load shared files');
    } finally {
      setLoading(false);
    }
  };

  const fetchFolders = async () => {
    try {
      const response = await folderAPI.getAll();
      setFolders(response.data || []);
    } catch (err) {
      console.error('Failed to fetch folders:', err);
    }
  };

  const isImageFile = (contentType) => {
    return contentType && contentType.startsWith('image/');
  };

  const isPreviewable = (contentType) => {
    return isImageFile(contentType);
  };

  const handlePreview = async (file) => {
    if (!isPreviewable(file.contentType)) {
      setError('Preview not available for this file type');
      return;
    }

    try {
      const response = await fileAPI.preview(file.fileId);
      const blob = new Blob([response.data], { type: file.contentType });
      const url = URL.createObjectURL(blob);
      setPreviewUrl(url);
      setPreviewFile(file);
    } catch (err) {
      setError('Failed to preview file');
    }
  };

  const handleDownload = async (file) => {
    if (file.access !== 'WRITE') {
      setError('Download is only available for files with WRITE access');
      return;
    }

    try {
      const response = await fileAPI.download(file.fileId);
      const blob = new Blob([response.data], { type: file.contentType });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to download file');
    }
  };

  const handleMoveClick = async (file) => {
    if (file.access !== 'WRITE') {
      setError('Saving to your folders is only available for files with WRITE access');
      return;
    }
    setSelectedFile(file);
    setShowMoveDialog(true);
    await fetchFolders();
  };

  const handleMove = async () => {
    if (!selectedFile || !targetFolderId) {
      setError('Please select a folder');
      return;
    }

    setMoving(true);
    setError('');

    try {
      await fileAPI.copySharedFile(selectedFile.fileId, targetFolderId);
      setShowMoveDialog(false);
      setSelectedFile(null);
      setTargetFolderId(null);
      setError('');
      // Refresh shared files list
      fetchSharedFiles();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save file');
    } finally {
      setMoving(false);
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-lg text-gray-500">Loading shared files...</div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {sharedFiles.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 text-lg">No shared files yet</p>
          <p className="text-gray-400 text-sm mt-2">Files shared with you will appear here after you accept them</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {sharedFiles.map((file) => (
            <div
              key={file.permissionId}
              className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow p-6"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-gray-800 truncate">
                    {file.fileName}
                  </h3>
                  <p className="text-sm text-gray-500 mt-1">
                    {formatFileSize(file.fileSize)}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    Shared by: {file.ownerUsername}
                  </p>
                  <p className="text-xs text-gray-400">
                    From: {file.folderName}
                  </p>
                </div>
                <div className="flex items-center gap-1">
                  <span className={`text-xs px-2 py-1 rounded ${
                    file.access === 'WRITE' 
                      ? 'bg-green-100 text-green-700' 
                      : 'bg-blue-100 text-blue-700'
                  }`}>
                    {file.access}
                  </span>
                </div>
              </div>

              <div className="flex gap-2 mt-4">
                {isPreviewable(file.contentType) && (
                  <button
                    onClick={() => handlePreview(file)}
                    className="flex-1 px-3 py-2 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700 transition-colors"
                  >
                    Preview
                  </button>
                )}
                {file.access === 'WRITE' && (
                  <>
                    <button
                      onClick={() => handleDownload(file)}
                      className="flex-1 px-3 py-2 bg-green-600 text-white text-sm rounded hover:bg-green-700 transition-colors"
                    >
                      Download
                    </button>
                    <button
                      onClick={() => handleMoveClick(file)}
                      className="flex-1 px-3 py-2 bg-purple-600 text-white text-sm rounded hover:bg-purple-700 transition-colors"
                    >
                      Save
                    </button>
                  </>
                )}
                {file.access === 'READ' && (
                  <button
                    disabled
                    className="flex-1 px-3 py-2 bg-gray-300 text-gray-500 text-sm rounded cursor-not-allowed"
                    title="Download requires WRITE access"
                  >
                    Preview Only
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Preview Modal */}
      {previewFile && previewUrl && (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-4xl max-h-[90vh] p-4">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">{previewFile.fileName}</h3>
              <button
                onClick={() => {
                  setPreviewFile(null);
                  if (previewUrl) {
                    URL.revokeObjectURL(previewUrl);
                    setPreviewUrl(null);
                  }
                }}
                className="text-gray-500 hover:text-gray-700"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="max-h-[80vh] overflow-auto">
              {isImageFile(previewFile.contentType) && (
                <img src={previewUrl} alt={previewFile.fileName} className="max-w-full h-auto" />
              )}
            </div>
          </div>
        </div>
      )}

      {/* Save to My Folders Dialog */}
      {showMoveDialog && selectedFile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">
              Save to My Folders
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Select a folder to copy &quot;{selectedFile.fileName}&quot; to:
            </p>

            <select
              value={targetFolderId || ''}
              onChange={(e) => setTargetFolderId(e.target.value ? Number(e.target.value) : null)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent mb-4"
            >
              <option value="">Select a folder...</option>
              {folders.map((folder) => (
                <option key={folder.id} value={folder.id}>
                  {folder.name}
                </option>
              ))}
            </select>

            {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded mb-4 text-sm">
                {error}
              </div>
            )}

            <div className="flex justify-end space-x-3">
              <button
                onClick={() => {
                  setShowMoveDialog(false);
                  setSelectedFile(null);
                  setTargetFolderId(null);
                  setError('');
                }}
                className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleMove}
                disabled={moving || !targetFolderId}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {moving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SharedFilesSection;

