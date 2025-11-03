package com.rip.vaultify.controller;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FileRepository;
import com.rip.vaultify.repository.UserRepository;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @PostMapping("/share")
    public ResponseEntity<?> share(@RequestBody Map<String, String> body){
        try {
            // Get current authenticated user (the person trying to share)
            User currentUser = userService.getCurrentUser();
            
            Long fileId = Long.valueOf(body.get("fileId"));
            String username = body.get("username");
            String accessStr = body.get("access");

            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            User targetUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            // Validate access type
            Permission.Access access;
            try {
                access = Permission.Access.valueOf(accessStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid access type. Must be READ or WRITE"));
            }
            
            // Share file (this will validate that currentUser is the owner)
            Permission p = permissionService.share(file, targetUser, access, currentUser);

            return ResponseEntity.ok(Map.of(
                    "id", p.getId(),
                    "access", p.getAccess().name(),
                    "message", "File shared successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/file/{fileId}")
    public ResponseEntity<?> getFilePermissions(@PathVariable Long fileId) {
        try {
            User currentUser = userService.getCurrentUser();
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            // Only owner can view permissions
            if (!permissionService.isOwner(file, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only the file owner can view permissions"));
            }
            
            return ResponseEntity.ok(permissionService.getFilePermissionsWithUserInfo(file));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/shared")
    public ResponseEntity<?> getSharedFiles() {
        try {
            User currentUser = userService.getCurrentUser();
            List<Permission> sharedPermissions = permissionService.getSharedFilesForUser(currentUser);
            
            List<Map<String, Object>> sharedFiles = sharedPermissions.stream()
                    .map(p -> {
                        File file = p.getFile();
                        User owner = permissionService.getFileOwner(file).orElse(null);
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("permissionId", p.getId());
                        fileInfo.put("fileId", file.getId());
                        fileInfo.put("fileName", file.getOriginalName());
                        fileInfo.put("fileSize", file.getSize());
                        fileInfo.put("contentType", file.getContentType());
                        fileInfo.put("uploadedAt", file.getUploadedAt().toString());
                        fileInfo.put("access", p.getAccess().name());
                        fileInfo.put("folderId", file.getFolder().getId());
                        fileInfo.put("folderName", file.getFolder().getName());
                        if (owner != null) {
                            fileInfo.put("ownerUsername", owner.getUsername());
                            fileInfo.put("ownerId", owner.getId());
                        }
                        return fileInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(sharedFiles);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/file/{fileId}/owner")
    public ResponseEntity<?> getFileOwner(@PathVariable Long fileId) {
        try {
            User currentUser = userService.getCurrentUser();
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            // Check if user has permission to view this file
            if (!permissionService.isOwner(file, currentUser) && 
                !permissionService.hasReadPermission(file, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to view this file"));
            }
            
            User owner = permissionService.getFileOwner(file)
                    .orElseThrow(() -> new RuntimeException("File owner not found"));
            
            Map<String, Object> ownerInfo = new HashMap<>();
            ownerInfo.put("id", owner.getId());
            ownerInfo.put("username", owner.getUsername());
            
            return ResponseEntity.ok(ownerInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/viewed/{permissionId}")
    public ResponseEntity<?> markPermissionAsViewed(@PathVariable Long permissionId) {
        try {
            User currentUser = userService.getCurrentUser();
            permissionService.markPermissionAsViewed(permissionId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Permission marked as viewed"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
