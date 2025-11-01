package com.rip.vaultify.service;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FileRepository;
import com.rip.vaultify.repository.FolderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
    }

    @Transactional
    public File uploadFile(MultipartFile multipartFile, Long folderId, Long userId) throws IOException {
        // Validate folder exists and belongs to user
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + folderId));
        
        if (!folder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Folder does not belong to user");
        }
        
        User user = folder.getUser();

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique file name
        String originalName = multipartFile.getOriginalFilename();
        String storedName = UUID.randomUUID().toString() + "_" + originalName;
        Path filePath = uploadPath.resolve(storedName);

        // Save file to disk
        Files.copy(multipartFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create file entity
        File file = new File(
                originalName,
                storedName,
                multipartFile.getContentType(),
                multipartFile.getSize(),
                filePath.toString(),
                folder,
                user
        );

        return fileRepository.save(file);
    }

    public List<File> getFilesByFolder(Long folderId, Long userId) {
        return fileRepository.findByFolderIdAndUserId(folderId, userId);
    }

    public File getFileByIdAndUser(Long id, Long userId) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
        if (!file.getUser().getId().equals(userId)) {
            throw new RuntimeException("File does not belong to user");
        }
        return file;
    }

    @Transactional
    public void deleteFile(Long id, Long userId) throws IOException {
        File file = getFileByIdAndUser(id, userId);

        // Delete physical file
        Path filePath = Paths.get(file.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        fileRepository.delete(file);
    }

    public byte[] downloadFile(Long id, Long userId) throws IOException {
        File file = getFileByIdAndUser(id, userId);
        Path filePath = Paths.get(file.getFilePath());
        return Files.readAllBytes(filePath);
    }
}