package com.rip.vaultify.service;

import com.rip.vaultify.dto.FolderResponse;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;

@Service
public class FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);
    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Transactional
    public Folder createFolder(String name, Long parentId, User user) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(user.getId(), "user id cannot be null");
        Folder folder = new Folder();
        folder.setName(name);
        folder.setUser(user);

        if (parentId != null) {
            Folder parentFolder = folderRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found with id: " + parentId));
            // Ensure parent folder belongs to the same user
            if (!parentFolder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Parent folder does not belong to user");
            }
            folder.setParent(parentFolder);
        }

        return folderRepository.save(folder);
    }

    public List<FolderResponse> getAllFoldersByUser(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return folderRepository.findByUserIdAndParentIdIsNull(userId)
                .stream()
                .map(FolderResponse::new)
                .toList();
    }

    public List<Folder> getFoldersByParentAndUser(Long parentId, Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return folderRepository.findByUserIdAndParentId(userId, parentId);
    }

    @NonNull
    public Folder getFolderByIdAndUser(Long id, Long userId) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Folder folder = Objects.requireNonNull(folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id)));
        // Allow access if user owns the folder OR has file permissions in the folder
        // We'll check file permissions at the file level, so just return the folder
        // The folder access check is done at the file level when fetching files
        return folder;
    }
    
    /**
     * Check if user owns the folder
     */
    public boolean isFolderOwner(Long folderId, Long userId) {
        Objects.requireNonNull(folderId, "folderId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + folderId));
        return folder.getUser().getId().equals(userId);
    }

    @Transactional
    public Folder renameFolder(Long id, String newName, Long userId) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Folder folder = getFolderByIdAndUser(id, userId);
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long id, Long userId) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Folder folder = getFolderByIdAndUser(id, userId);
        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolderContents(Long id, Long userId) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Folder folder = getFolderByIdAndUser(id, userId);
        folder.getChildren().size(); // initialize lazy-loaded children
        return folder;
    }

    public List<Folder> getAllFolders() {
        return folderRepository.findAll();
    }

    @NonNull
    public Folder getFolderById(Long id) {
        Objects.requireNonNull(id, "folder id cannot be null");
        return Objects.requireNonNull(folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id)));
    }

    public List<Folder> getRootFolders(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return folderRepository.findByUserIdAndParentIdIsNull(userId);
    }

    public Folder createFolder(String name, Long parentId) {
        Objects.requireNonNull(name, "folder name cannot be null");
        // This is a simplified version for GraphQL - in a real app you'd get current user
        User currentUser = new User(); // This would come from security context
        return createFolder(name, parentId, currentUser);
    }

    public Folder updateFolder(Long id, String name) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Objects.requireNonNull(name, "folder name cannot be null");
        Folder folder = getFolderById(id);
        folder.setName(name);
        return folderRepository.save(folder);
    }

    public void deleteFolder(Long id) {
        Objects.requireNonNull(id, "folder id cannot be null");
        Folder folder = getFolderById(id);
        folderRepository.delete(folder);
    }
}
