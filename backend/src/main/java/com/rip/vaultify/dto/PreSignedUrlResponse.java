package com.rip.vaultify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreSignedUrlResponse {
    private String token;
    private String url;
    private Long expiresInSeconds;
}

