package com.rip.vaultify.dto;

import com.rip.vaultify.model.File;
import java.time.LocalDateTime;

public class FileResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
    private Long folderId;
    private String folderName;

    public FileResponse(File file) {
        this.id = file.getId();
        this.originalName = file.getOriginalName();
        this.contentType = file.getContentType();
        this.size = file.getSize();
        this.uploadedAt = file.getUploadedAt();
        this.folderId = file.getFolder().getId();
        this.folderName = file.getFolder().getName();
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public Long getSize() { return size; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public Long getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }
}