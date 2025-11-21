package com.rip.vaultify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata implements Serializable {
    private Long id;
    private String originalName;
    private Long size;
    private Long userId;
    private String contentType;
    private String etag;
    private Long lastModified;
}