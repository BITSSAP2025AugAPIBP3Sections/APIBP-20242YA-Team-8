package com.rip.vaultify.controller;

import com.rip.vaultify.config.LoggingConfig;
import com.rip.vaultify.dto.FileResponse;
import com.rip.vaultify.model.File;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final Marker FILE_OPERATION_MARKER = MarkerFactory.getMarker("FILE_OPERATION");
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");
    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("AUDIT");

    private final FileService fileService;
    private final UserService userService;
    private final PermissionService permissionService;

    public FileController(FileService fileService, UserService userService, PermissionService permissionService) {
        this.fileService = fileService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId) throws IOException {
        
        User currentUser = userService.getCurrentUser();
        LoggingConfig.LoggingContext.setUserId(currentUser.getId());
        LoggingConfig.LoggingContext.addContext("action", "file_upload");
        LoggingConfig.LoggingContext.addContext("folderId", folderId.toString());
        LoggingConfig.LoggingContext.addContext("fileName", file.getOriginalFilename());
        LoggingConfig.LoggingContext.addContext("fileSize", String.valueOf(file.getSize()));
        LoggingConfig.LoggingContext.addContext("contentType", file.getContentType());
        
        logger.info(FILE_OPERATION_MARKER, "File upload started - fileName: {}, size: {} bytes, folderId: {}, user: {}", 
                   file.getOriginalFilename(), file.getSize(), folderId, currentUser.getId());
        
        try {
            File uploadedFile = fileService.uploadFile(file, folderId, currentUser.getId());
            LoggingConfig.LoggingContext.addContext("fileId", uploadedFile.getId().toString());
            
            logger.info(AUDIT_MARKER, "File upload successful - fileId: {}, fileName: {}, size: {} bytes, user: {}", 
                       uploadedFile.getId(), uploadedFile.getOriginalName(), uploadedFile.getSize(), currentUser.getId());
            
            return ResponseEntity.ok(new FileResponse(uploadedFile));
        } catch (Exception e) {
            logger.error(FILE_OPERATION_MARKER, "File upload failed - fileName: {}, folderId: {}, user: {}, error: {}", 
                        file.getOriginalFilename(), folderId, currentUser.getId(), e.getMessage(), e);
            throw e;
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("folderId");
            LoggingConfig.LoggingContext.removeContext("fileName");
            LoggingConfig.LoggingContext.removeContext("fileSize");
            LoggingConfig.LoggingContext.removeContext("contentType");
            LoggingConfig.LoggingContext.removeContext("fileId");
        }
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileResponse>> getFilesByFolder(@PathVariable Long folderId) {
        User currentUser = userService.getCurrentUser();
        List<FileResponse> files = fileService.getFilesByFolder(folderId, currentUser.getId())
                .stream()
                .map(file -> new FileResponse(file, currentUser, permissionService))
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        return ResponseEntity.ok(new FileResponse(file, currentUser, permissionService));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) throws IOException {
        User currentUser = userService.getCurrentUser();
        fileService.deleteFile(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ByteArrayResource> previewFile(@PathVariable Long id) throws IOException {
        User currentUser = userService.getCurrentUser();
        // getFileByIdAndUser already checks READ permission
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        byte[] data = fileService.downloadFile(id, currentUser.getId());

        ByteArrayResource resource = new ByteArrayResource(data);

        // Preview: no attachment header, allows READ users
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(resource);
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) throws IOException {
        User currentUser = userService.getCurrentUser();
        LoggingConfig.LoggingContext.setUserId(currentUser.getId());
        LoggingConfig.LoggingContext.addContext("action", "file_download");
        LoggingConfig.LoggingContext.addContext("fileId", id.toString());
        
        logger.info(FILE_OPERATION_MARKER, "File download attempt - fileId: {}, user: {}", id, currentUser.getId());
        
        try {
            File file = fileService.getFileByIdAndUser(id, currentUser.getId());
            LoggingConfig.LoggingContext.addContext("fileName", file.getOriginalName());
            LoggingConfig.LoggingContext.addContext("fileSize", String.valueOf(file.getSize()));
            
            // Check if user has WRITE permission (required for download)
            User user = new User();
            user.setId(currentUser.getId());
            if (!permissionService.isOwner(file, user) && !permissionService.hasWritePermission(file, user)) {
                logger.warn(SECURITY_MARKER, "File download denied - insufficient permissions - fileId: {}, fileName: {}, user: {}", 
                           id, file.getOriginalName(), currentUser.getId());
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .build();
            }
            
            byte[] data = fileService.downloadFile(id, currentUser.getId());
            ByteArrayResource resource = new ByteArrayResource(data);
            
            logger.info(AUDIT_MARKER, "File download successful - fileId: {}, fileName: {}, size: {} bytes, user: {}", 
                       id, file.getOriginalName(), file.getSize(), currentUser.getId());

            // Download: with attachment header, requires WRITE permission
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error(FILE_OPERATION_MARKER, "File download failed - fileId: {}, user: {}, error: {}", 
                        id, currentUser.getId(), e.getMessage(), e);
            throw e;
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("fileId");
            LoggingConfig.LoggingContext.removeContext("fileName");
            LoggingConfig.LoggingContext.removeContext("fileSize");
        }
    }
    
    @PostMapping("/copy")
    public ResponseEntity<FileResponse> copySharedFile(
            @RequestBody Map<String, Long> body) throws IOException {
        User currentUser = userService.getCurrentUser();
        Long fileId = body.get("fileId");
        Long folderId = body.get("folderId");
        
        if (fileId == null || folderId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        File copiedFile = fileService.copySharedFileToFolder(fileId, folderId, currentUser.getId());
        return ResponseEntity.ok(new FileResponse(copiedFile, currentUser, permissionService));
    }

}
