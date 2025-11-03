package com.rip.vaultify.dto;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.PermissionService;
import java.time.LocalDateTime;

public class FileResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
    private Long folderId;
    private String folderName;
    private Boolean isShared;
    private String ownerUsername;
    private Long ownerId;

    public FileResponse(File file) {
        this.id = file.getId();
        this.originalName = file.getOriginalName();
        this.contentType = file.getContentType();
        this.size = file.getSize();
        this.uploadedAt = file.getUploadedAt();
        this.folderId = file.getFolder().getId();
        this.folderName = file.getFolder().getName();
        this.isShared = false;
        this.ownerUsername = file.getUser().getUsername();
        this.ownerId = file.getUser().getId();
    }

    public FileResponse(File file, User currentUser, PermissionService permissionService) {
        this.id = file.getId();
        this.originalName = file.getOriginalName();
        this.contentType = file.getContentType();
        this.size = file.getSize();
        this.uploadedAt = file.getUploadedAt();
        this.folderId = file.getFolder().getId();
        this.folderName = file.getFolder().getName();
        
        // Check if current user is owner or has shared access
        boolean isOwner = permissionService.isOwner(file, currentUser);
        boolean hasRead = permissionService.hasReadPermission(file, currentUser);
        // File is shared if user has read permission but is not the owner
        this.isShared = Boolean.valueOf(!isOwner && hasRead);
        
        // Set owner info
        User owner = permissionService.getFileOwner(file).orElse(file.getUser());
        this.ownerUsername = owner.getUsername();
        this.ownerId = owner.getId();
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public Long getSize() { return size; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public Long getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }
    public Boolean getIsShared() { return isShared; }
    public String getOwnerUsername() { return ownerUsername; }
    public Long getOwnerId() { return ownerId; }
    
    public void setIsShared(Boolean isShared) { this.isShared = isShared; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}