package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FileResponse;
import com.rip.vaultify.model.File;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.IdempotencyService;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UserService userService;
    private final PermissionService permissionService;
    private final IdempotencyService idempotencyService;

    public FileController(FileService fileService, UserService userService, 
                         PermissionService permissionService, IdempotencyService idempotencyService) {
        this.fileService = fileService;
        this.userService = userService;
        this.permissionService = permissionService;
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Generate ETag from file metadata
     */
    private String generateETag(File file) {
        try {
            String data = file.getId() + "_" + file.getUploadedAt() + "_" + file.getSize();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "\"" + hexString.toString() + "\"";
        } catch (Exception e) {
            return "\"" + file.getId() + "_" + file.getUploadedAt().hashCode() + "\"";
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        
        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            FileResponse cached = idempotencyService.getCachedResponse(idempotencyKey, FileResponse.class);
            if (cached != null) {
                return ResponseEntity.ok(cached);
            }
        }
        
        User currentUser = userService.getCurrentUser();
        File uploadedFile = fileService.uploadFile(file, folderId, currentUser.getId());
        FileResponse response = new FileResponse(uploadedFile);
        
        // Store in idempotency cache if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            idempotencyService.storeResponse(idempotencyKey, response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileResponse>> getFilesByFolder(
            @PathVariable Long folderId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        List<FileResponse> files = fileService.getFilesByFolder(folderId, currentUser.getId());
        
        // Generate ETag for the list
        String etag = generateETagForList(files);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(files);
    }
    
    private String generateETagForList(List<FileResponse> files) {
        try {
            StringBuilder data = new StringBuilder();
            for (FileResponse file : files) {
                data.append(file.getId()).append("_").append(file.getSize());
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "\"" + hexString.toString() + "\"";
        } catch (Exception e) {
            return "\"" + files.hashCode() + "\"";
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(
            @PathVariable Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        FileResponse fileResponse = new FileResponse(file, currentUser, permissionService);
        
        String etag = generateETag(file);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(fileResponse);
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
        byte[] data = Objects.requireNonNull(fileService.downloadFile(id, currentUser.getId()), "File data cannot be null");

        ByteArrayResource resource = new ByteArrayResource(data);

        // Preview: no attachment header, allows READ users
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) throws IOException {
        User currentUser = userService.getCurrentUser();
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        
        // Check if user has WRITE permission (required for download)
        User user = new User();
        user.setId(currentUser.getId());
        if (!permissionService.isOwner(file, user) && !permissionService.hasWritePermission(file, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            ByteArrayResource cached = idempotencyService.getCachedResponse(idempotencyKey, ByteArrayResource.class);
            if (cached != null) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                        .body(cached);
            }
        }
        
        String etag = generateETag(file);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        byte[] data = Objects.requireNonNull(fileService.downloadFile(id, currentUser.getId()), "File data cannot be null");
        ByteArrayResource resource = new ByteArrayResource(data);
        
        // Store in idempotency cache if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            idempotencyService.storeResponse(idempotencyKey, resource);
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        MediaType mediaType = MediaType.parseMediaType(contentType);
        String downloadName = Objects.requireNonNullElse(file.getOriginalName(), "downloaded-file");

        return ResponseEntity.ok()
                .eTag(etag)
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
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