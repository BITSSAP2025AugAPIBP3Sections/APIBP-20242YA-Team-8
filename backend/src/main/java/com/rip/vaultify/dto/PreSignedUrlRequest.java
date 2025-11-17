package com.rip.vaultify.dto;

import lombok.Data;

@Data
public class PreSignedUrlRequest {
    private Long fileId;
    private String action; // "read" or "write"
}

