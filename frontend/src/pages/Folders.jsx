import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { folderAPI, permissionAPI } from '../services/api';
import SharingDialog from '../components/SharingDialog';
import SharedFilesNotification from '../components/SharedFilesNotification';

const Folders = () => {
  const [folders, setFolders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [sharingDialog, setSharingDialog] = useState({ isOpen: false, folderId: null });
  const [showSharedFiles, setShowSharedFiles] = useState(false);
  const [sharedFilesCount, setSharedFilesCount] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // Initial load
    fetchFolders();
    checkSharedFiles();
    
    // Periodic refresh every 30 seconds with subtle loading indicator
    const refreshInterval = setInterval(() => {
      refreshFolders();
    }, 30000); // Every 30 seconds
    
    // Check for shared files periodically
    const sharedFilesInterval = setInterval(checkSharedFiles, 30000);
    
    return () => {
      clearInterval(refreshInterval);
      clearInterval(sharedFilesInterval);
    };
  }, []);

  const checkSharedFiles = async () => {
    try {
      const response = await permissionAPI.getSharedFiles();
      setSharedFilesCount(response.data?.length || 0);
    } catch (err) {
      console.error('Failed to check shared files:', err);
    }
  };

  const fetchFolders = async () => {
    try {
      setLoading(true);
      const response = await folderAPI.getAll();
      // Filter to show only root folders (folders without a parent)
      const rootFolders = response.data.filter(folder => !folder.parent);
      setFolders(rootFolders);
      setError(''); // Clear any previous errors
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch folders');
    } finally {
      setLoading(false);
    }
  };

  const refreshFolders = async () => {
    try {
      setRefreshing(true);
      const response = await folderAPI.getAll();
      // Filter to show only root folders (folders without a parent)
      const rootFolders = response.data.filter(folder => !folder.parent);
      setFolders(rootFolders);
    } catch (err) {
      // Don't show error on background refresh to avoid disrupting user
      console.error('Background refresh failed:', err);
    } finally {
      setRefreshing(false);
    }
  };

  const handleCreateFolder = async (e) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;

    try {
      await folderAPI.create(newFolderName.trim());
      setNewFolderName('');
      setShowCreateDialog(false);
      fetchFolders();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create folder');
    }
  };

  const handleDeleteFolder = async (id) => {
    if (!window.confirm('Are you sure you want to delete this folder?')) return;

    try {
      await folderAPI.delete(id);
      fetchFolders();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete folder');
    }
  };

  const handleFolderClick = (folderId) => {
    navigate(`/folders/${folderId}/files`);
  };

  const handleShareClick = (e, folderId) => {
    e.stopPropagation();
    setSharingDialog({ isOpen: true, folderId });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading folders...</div>
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
              <h1 className="text-2xl font-bold text-gray-800">Vaultify</h1>
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
          <div className="flex items-center gap-3">
            <h2 className="text-3xl font-bold text-gray-800">My Folders</h2>
            {refreshing && (
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Refreshing...</span>
              </div>
            )}
          </div>
          <div className="flex gap-3 items-center">
            {sharedFilesCount > 0 && (
              <button
                onClick={() => setShowSharedFiles(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                Shared Files
                {sharedFilesCount > 0 && (
                  <span className="bg-white text-blue-600 text-xs font-bold px-2 py-0.5 rounded-full">
                    {sharedFilesCount}
                  </span>
                )}
              </button>
            )}
            <button
              onClick={() => setShowCreateDialog(true)}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
            >
              + Create Folder
            </button>
            <button
              onClick={refreshFolders}
              disabled={refreshing}
              className="px-3 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors disabled:opacity-50"
              title="Refresh folders"
            >
              <svg className={`w-5 h-5 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>

        {/* Folders Grid */}
        {folders.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg mb-4">No folders yet</p>
            <button
              onClick={() => setShowCreateDialog(true)}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
            >
              Create Your First Folder
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {folders.map((folder) => (
              <div
                key={folder.id}
                className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow cursor-pointer relative group"
                onClick={() => handleFolderClick(folder.id)}
              >
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-800 truncate">
                        {folder.name}
                      </h3>
                      <p className="text-sm text-gray-500 mt-1">
                        {folder.children?.length || 0} subfolders
                      </p>
                    </div>
                    <button
                      onClick={(e) => handleShareClick(e, folder.id)}
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
                        handleDeleteFolder(folder.id);
                      }}
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

      {/* Create Folder Dialog */}
      {showCreateDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Create New Folder</h3>
            <form onSubmit={handleCreateFolder}>
              <input
                type="text"
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                placeholder="Folder name"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent mb-4"
                autoFocus
              />
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateDialog(false);
                    setNewFolderName('');
                  }}
                  className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
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
        onClose={() => setSharingDialog({ isOpen: false, folderId: null })}
        folderId={sharingDialog.folderId}
      />

      {/* Shared Files Notification */}
      <SharedFilesNotification
        isOpen={showSharedFiles}
        onClose={() => {
          setShowSharedFiles(false);
          checkSharedFiles();
          // Refresh folders after accepting files
          refreshFolders();
        }}
      />
    </div>
  );
};

export default Folders;
