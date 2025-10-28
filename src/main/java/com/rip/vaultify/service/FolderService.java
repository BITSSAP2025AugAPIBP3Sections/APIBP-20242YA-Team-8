package com.rip.vaultify.service;

import com.rip.vaultify.model.Folder;
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

    // ✅ Create folder (with optional parent)
    @Transactional
    public Folder createFolder(String name, Long parentId) {
        Folder folder = new Folder();
        folder.setName(name);

        if (parentId != null) {
            Folder parentFolder = folderRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found with id: " + parentId));
            folder.setParent(parentFolder);
        }

        return folderRepository.save(folder);
    }

    // ✅ Get all folders
    public List<Folder> getAllFolders() {
        return folderRepository.findAll();
    }

    // ✅ Get folder by ID
    public Folder getFolderById(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));
    }

    // ✅ Rename existing folder
    @Transactional
    public Folder renameFolder(Long id, String newName) {
        Folder folder = getFolderById(id);
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    // ✅ Delete folder by ID
    @Transactional
    public void deleteFolder(Long id) {
        Folder folder = getFolderById(id);
        folderRepository.delete(folder);
    }

    // ✅ Optionally fetch folder contents recursively
    @Transactional(readOnly = true)
    public Folder getFolderContents(Long id) {
        Folder folder = getFolderById(id);
        folder.getChildren().size(); // initialize lazy-loaded children
        return folder;
    }
}
