package com.rip.vaultify.controller;

import com.rip.vaultify.dto.PreSignedUrlRequest;
import com.rip.vaultify.dto.PreSignedUrlResponse;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.PreSignedUrlService;
import com.rip.vaultify.service.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files/presign")
public class PreSignedUrlController {

    private final PreSignedUrlService preSignedUrlService;
    private final FileService fileService;
    private final UserService userService;

    public PreSignedUrlController(PreSignedUrlService preSignedUrlService,
                                 FileService fileService,
                                 UserService userService) {
        this.preSignedUrlService = preSignedUrlService;
        this.fileService = fileService;
        this.userService = userService;
    }

    /**
     * Generate pre-signed upload URL
     */
    @PostMapping("/upload")
    public ResponseEntity<PreSignedUrlResponse> generateUploadUrl(@RequestBody PreSignedUrlRequest request) {
        User currentUser = userService.getCurrentUser();
        PreSignedUrlResponse response = preSignedUrlService.generatePreSignedUrl(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate pre-signed download URL
     */
    @PostMapping("/download")
    public ResponseEntity<PreSignedUrlResponse> generateDownloadUrl(@RequestBody PreSignedUrlRequest request) {
        User currentUser = userService.getCurrentUser();
        PreSignedUrlResponse response = preSignedUrlService.generatePreSignedUrl(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Execute pre-signed download
     */
    @GetMapping("/read")
    public ResponseEntity<ByteArrayResource> executePreSignedDownload(
            @RequestParam String token) throws IOException {
        // Validate token
        Map<String, Object> tokenData = preSignedUrlService.validateToken(token);
        Long fileId = ((Number) tokenData.get("fileId")).longValue();
        String action = (String) tokenData.get("action");
        
        if (!"read".equals(action)) {
            throw new RuntimeException("Token is not valid for read operation");
        }
        
        // Get file and download
        Long userId = ((Number) tokenData.get("userId")).longValue();
        var file = fileService.getFileByIdAndUser(fileId, userId);
        byte[] data = fileService.downloadFile(fileId, userId);
        ByteArrayResource resource = new ByteArrayResource(data);
        
        // Invalidate token after use (one-time use)
        preSignedUrlService.invalidateToken(token);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }

    /**
     * Execute pre-signed upload
     */
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> executePreSignedUpload(
            @RequestParam String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId) throws IOException {
        // Validate token
        Map<String, Object> tokenData = preSignedUrlService.validateToken(token);
        Long fileId = ((Number) tokenData.get("fileId")).longValue();
        String action = (String) tokenData.get("action");
        
        if (!"write".equals(action)) {
            throw new RuntimeException("Token is not valid for write operation");
        }
        
        // Verify file ID matches (if provided)
        if (fileId != null) {
            // This could be for updating an existing file
            // For now, we'll just upload to the specified folder
        }
        
        // Upload file
        Long userId = ((Number) tokenData.get("userId")).longValue();
        var uploadedFile = fileService.uploadFile(file, folderId, userId);
        
        // Invalidate token after use (one-time use)
        preSignedUrlService.invalidateToken(token);
        
        return ResponseEntity.ok(Map.of(
                "id", uploadedFile.getId(),
                "originalName", uploadedFile.getOriginalName(),
                "size", uploadedFile.getSize()
        ));
    }
}

