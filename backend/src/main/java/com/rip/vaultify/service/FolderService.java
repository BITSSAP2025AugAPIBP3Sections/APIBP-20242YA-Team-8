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

    public List<Folder> getAllFolders() {
        return folderRepository.findAll();
    }

    public Folder getFolderById(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));
    }

    @Transactional
    public Folder renameFolder(Long id, String newName) {
        Folder folder = getFolderById(id);
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long id) {
        Folder folder = getFolderById(id);
        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Folder getFolderContents(Long id) {
        Folder folder = getFolderById(id);
        folder.getChildren().size(); // initialize lazy-loaded children
        return folder;
    }
}
