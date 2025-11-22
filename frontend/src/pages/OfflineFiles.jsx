import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllOfflineFiles, getOfflineFile, removeOfflineFile } from '../services/offlineStorage';
import { useOfflineFiles } from '../hooks/useFiles';

const OfflineFiles = () => {
  const [offlineFiles, setOfflineFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const navigate = useNavigate();

  const { data: files = [], refetch } = useOfflineFiles();

  useEffect(() => {
    loadOfflineFiles();
  }, []);

  useEffect(() => {
    if (files.length > 0) {
      setOfflineFiles(files);
    }
  }, [files]);

  const loadOfflineFiles = async () => {
    try {
      setLoading(true);
      const files = await getAllOfflineFiles();
      setOfflineFiles(files);
    } catch (err) {
      console.error('Failed to load offline files:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewFile = async (file) => {
    try {
      const blob = await getOfflineFile(file.fileId);
      if (blob) {
        const url = URL.createObjectURL(blob);
        setPreviewUrl(url);
        setSelectedFile(file);
      }
    } catch (err) {
      console.error('Failed to load file:', err);
      alert('Failed to load file');
    }
  };

  const handleDownloadFile = async (file) => {
    try {
      const blob = await getOfflineFile(file.fileId);
      if (blob) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = file.originalName || 'download';
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      console.error('Failed to download file:', err);
      alert('Failed to download file');
    }
  };

  const handleRemoveOffline = async (fileId) => {
    if (!window.confirm('Remove this file from offline storage?')) return;
    
    try {
      await removeOfflineFile(fileId);
      await loadOfflineFiles();
      if (selectedFile?.fileId === fileId) {
        setSelectedFile(null);
        if (previewUrl) {
          URL.revokeObjectURL(previewUrl);
          setPreviewUrl(null);
        }
      }
    } catch (err) {
      console.error('Failed to remove file:', err);
      alert('Failed to remove file');
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown';
    return new Date(timestamp).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Cleanup preview URL on unmount
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100">
        <div className="text-lg">Loading offline files...</div>
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
              <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Offline Files
              </h1>
              <p className="text-sm text-gray-600 mt-1">
                {offlineFiles.length} file{offlineFiles.length !== 1 ? 's' : ''} available offline
              </p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => navigate('/login')}
                className="px-4 py-2 text-indigo-600 bg-indigo-50 rounded-lg hover:bg-indigo-100 transition-colors"
              >
                Go to Login
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {offlineFiles.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <p className="text-gray-500 text-lg mb-4">No offline files available</p>
            <p className="text-gray-400 text-sm">
              Files you mark as "Offline" will appear here and be accessible even without internet.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {offlineFiles.map((file) => (
              <div
                key={file.fileId}
                className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
              >
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2">
                        <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <h3 className="text-lg font-semibold text-gray-800 truncate">
                          {file.originalName || 'Unknown File'}
                        </h3>
                      </div>
                      <p className="text-sm text-gray-500">
                        {formatFileSize(file.size)}
                      </p>
                      <p className="text-xs text-gray-400 mt-1">
                        Cached: {formatDate(file.cachedAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center justify-between mt-4 gap-2">
                    <button
                      onClick={() => handleViewFile(file)}
                      className="flex-1 text-indigo-600 hover:text-indigo-700 text-sm font-medium px-2 py-1 rounded hover:bg-indigo-50"
                    >
                      View
                    </button>
                    <button
                      onClick={() => handleDownloadFile(file)}
                      className="flex-1 text-green-600 hover:text-green-700 text-sm font-medium px-2 py-1 rounded hover:bg-green-50"
                    >
                      Download
                    </button>
                    <button
                      onClick={() => handleRemoveOffline(file.fileId)}
                      className="text-red-600 hover:text-red-700 text-sm px-2 py-1 rounded hover:bg-red-50"
                      title="Remove from offline storage"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Preview Modal */}
        {selectedFile && previewUrl && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] flex flex-col">
              <div className="flex justify-between items-center p-4 border-b">
                <h3 className="text-lg font-semibold text-gray-800">
                  {selectedFile.originalName}
                </h3>
                <button
                  onClick={() => {
                    setSelectedFile(null);
                    URL.revokeObjectURL(previewUrl);
                    setPreviewUrl(null);
                  }}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div className="flex-1 overflow-auto p-4">
                {selectedFile.contentType?.startsWith('image/') ? (
                  <img src={previewUrl} alt={selectedFile.originalName} className="max-w-full h-auto mx-auto" />
                ) : selectedFile.contentType?.startsWith('text/') ? (
                  <iframe src={previewUrl} className="w-full h-full min-h-[400px] border-0" />
                ) : (
                  <div className="text-center py-12">
                    <p className="text-gray-500 mb-4">Preview not available for this file type</p>
                    <button
                      onClick={() => handleDownloadFile(selectedFile)}
                      className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                    >
                      Download to View
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default OfflineFiles;


