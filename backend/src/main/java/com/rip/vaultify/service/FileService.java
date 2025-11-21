package com.rip.vaultify.service;

import com.rip.vaultify.dto.FileResponse;
import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FileRepository;
import com.rip.vaultify.repository.FolderRepository;
import com.rip.vaultify.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final PermissionService permissionService;
    private final PermissionRepository permissionRepository;

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository, PermissionService permissionService, PermissionRepository permissionRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.permissionService = permissionService;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public File uploadFile(MultipartFile multipartFile, Long folderId, Long userId) throws IOException {
        Objects.requireNonNull(folderId, "folderId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
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

        File savedFile = fileRepository.save(file);
        
        // Create OWNER permission for the file creator
        permissionService.createOwnerPermission(savedFile, user);
        
        return savedFile;
    }

    public List<FileResponse> getFilesByFolder(Long folderId, Long userId) {
        Objects.requireNonNull(folderId, "folderId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        
        User user = new User();
        user.setId(userId);

        List<File> allFiles = fileRepository.findByFolderId(folderId);

        return allFiles.stream()
                .filter(file -> permissionService.isOwner(file, user) ||
                        permissionService.hasReadPermission(file, user))
                .map(file -> new FileResponse(file, user, permissionService))
                .toList();
    }

    public File getFileByIdAndUser(Long id, Long userId) {
        Objects.requireNonNull(id, "file id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
        
        User user = new User();
        user.setId(userId);
        
        // Check if user has read permission (owner, read, or write)
        if (!permissionService.isOwner(file, user) && !permissionService.hasReadPermission(file, user)) {
            throw new RuntimeException("Access denied: You do not have permission to access this file");
        }
        
        return file;
    }
    
    public File getFileByIdForWrite(Long id, Long userId) {
        Objects.requireNonNull(id, "file id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
        
        User user = new User();
        user.setId(userId);
        
        // Check if user has write permission (owner or write)
        if (!permissionService.isOwner(file, user) && !permissionService.hasWritePermission(file, user)) {
            throw new RuntimeException("Access denied: You do not have write permission for this file");
        }
        
        return file;
    }

    @Transactional
    public void deleteFile(Long id, Long userId) throws IOException {
        Objects.requireNonNull(id, "file id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
        
        User user = new User();
        user.setId(userId);
        
        // Only owner can delete file
        if (!permissionService.isOwner(file, user)) {
            throw new RuntimeException("Access denied: Only the file owner can delete this file");
        }

        // Delete physical file
        Path filePath = Paths.get(file.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete any permissions referencing this file first to satisfy FK constraints
        permissionRepository.deleteByFile(file);

        // Delete from database
        fileRepository.delete(file);
    }

    public byte[] downloadFile(Long id, Long userId) throws IOException {
        Objects.requireNonNull(id, "file id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        // getFileByIdAndUser already checks read permission
        File file = getFileByIdAndUser(id, userId);
        Path filePath = Paths.get(file.getFilePath());
        return Files.readAllBytes(filePath);
    }
    
    /**
     * Copy a shared file to user's folder (only for WRITE users)
     * This creates a physical copy of the file in the user's folder
     */
    @Transactional
    public File copySharedFileToFolder(Long fileId, Long targetFolderId, Long userId) throws IOException {
        Objects.requireNonNull(fileId, "fileId cannot be null");
        Objects.requireNonNull(targetFolderId, "targetFolderId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        // Get the source file (check WRITE permission)
        File sourceFile = getFileByIdForWrite(fileId, userId);
        
        // Validate target folder exists and belongs to user
        Folder targetFolder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new RuntimeException("Target folder not found with id: " + targetFolderId));
        
        if (!targetFolder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Target folder does not belong to user");
        }
        
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Copy physical file
        Path sourceFilePath = Paths.get(sourceFile.getFilePath());
        String newStoredName = UUID.randomUUID().toString() + "_" + sourceFile.getOriginalName();
        Path newFilePath = uploadPath.resolve(newStoredName);
        Files.copy(sourceFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create new file entity in target folder
        File newFile = new File(
                sourceFile.getOriginalName(),
                newStoredName,
                sourceFile.getContentType(),
                sourceFile.getSize(),
                newFilePath.toString(),
                targetFolder,
                targetFolder.getUser() // The user who copied it
        );
        
        File savedFile = fileRepository.save(newFile);
        
        // Create OWNER permission for the user who copied it
        permissionService.createOwnerPermission(savedFile, targetFolder.getUser());
        
        return savedFile;
    }

    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }

    public File getFileById(Long id) {
        Objects.requireNonNull(id, "file id cannot be null");
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }

    public List<File> getFilesByFolderId(Long folderId) {
        Objects.requireNonNull(folderId, "folderId cannot be null");
        return fileRepository.findByFolderId(folderId);
    }
}
