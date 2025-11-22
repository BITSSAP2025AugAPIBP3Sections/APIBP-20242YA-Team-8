package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FolderRequest;
import com.rip.vaultify.dto.FolderResponse;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FolderService;
import com.rip.vaultify.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    public FolderController(FolderService folderService, UserService userService) {
        this.folderService = folderService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder folder = folderService.createFolder(request.getName(), request.getParentId(), currentUser);
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        List<FolderResponse> folders = folderService.getAllFoldersByUser(currentUser.getId());
        
        String etag = generateETagForList(folders);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(folders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(
            @PathVariable Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        Folder folder = folderService.getFolderByIdAndUser(id, currentUser.getId());
        FolderResponse response = new FolderResponse(folder);
        
        String etag = generateETag(folder);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(response);
    }
    
    private String generateETag(Folder folder) {
        try {
            String data = folder.getId() + "_" + folder.getName() + "_" + 
                         (folder.getParent() != null ? folder.getParent().getId() : "null");
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
            return "\"" + folder.getId() + "_" + folder.getName().hashCode() + "\"";
        }
    }
    
    private String generateETagForList(List<FolderResponse> folders) {
        try {
            StringBuilder data = new StringBuilder();
            for (FolderResponse folder : folders) {
                data.append(folder.getId()).append("_").append(folder.getName());
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
            return "\"" + folders.hashCode() + "\"";
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> renameFolder(@PathVariable Long id, @RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder updatedFolder = folderService.renameFolder(id, request.getName(), currentUser.getId());
        return ResponseEntity.ok(new FolderResponse(updatedFolder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        folderService.deleteFolder(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
