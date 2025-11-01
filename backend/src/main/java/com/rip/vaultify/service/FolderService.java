package com.rip.vaultify.service;

import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FolderService {

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Transactional
    public Folder createFolder(String name, Long parentId, User user) {
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

    public List<Folder> getAllFoldersByUser(Long userId) {
        return folderRepository.findByUserIdAndParentIdIsNull(userId);
    }

    public List<Folder> getFoldersByParentAndUser(Long parentId, Long userId) {
        return folderRepository.findByUserIdAndParentId(userId, parentId);
    }

    public Folder getFolderByIdAndUser(Long id, Long userId) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));
        if (!folder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Folder does not belong to user");
        }
        return folder;
    }

    @Transactional
    public Folder renameFolder(Long id, String newName, Long userId) {
        Folder folder = getFolderByIdAndUser(id, userId);
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long id, Long userId) {
        Folder folder = getFolderByIdAndUser(id, userId);
        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolderContents(Long id, Long userId) {
        Folder folder = getFolderByIdAndUser(id, userId);
        folder.getChildren().size(); // initialize lazy-loaded children
        return folder;
    }
}
