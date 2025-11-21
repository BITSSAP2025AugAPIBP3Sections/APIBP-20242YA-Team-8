package com.rip.vaultify.dto;

import lombok.Data;

@Data
public class PreSignedUrlRequest {
    private Long fileId;
    private Long folderId;
    private String action; // "read" or "write"
}
