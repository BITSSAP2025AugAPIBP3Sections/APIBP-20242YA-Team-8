package com.rip.vaultify.controller;

import com.rip.vaultify.dto.PreSignedUrlRequest;
import com.rip.vaultify.dto.PreSignedUrlResponse;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.IdempotencyService;
import com.rip.vaultify.service.PreSignedUrlService;
import com.rip.vaultify.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/files/presign")
@Tag(name = "Pre-Signed URLs", description = "Generate and use pre-signed URLs for secure file uploads and downloads")
@SecurityRequirement(name = "bearerAuth")
public class PreSignedUrlController {

    private final PreSignedUrlService preSignedUrlService;
    private final FileService fileService;
    private final UserService userService;
    private final IdempotencyService idempotencyService;

    public PreSignedUrlController(PreSignedUrlService preSignedUrlService,
                                  FileService fileService,
                                  UserService userService,
                                  IdempotencyService idempotencyService) {
        this.preSignedUrlService = preSignedUrlService;
        this.fileService = fileService;
        this.userService = userService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(
            summary = "Generate pre-signed upload URL",
            description = "Generates a time-limited, one-time-use token for secure file uploads. Requires folderId or fileId."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-signed URL generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreSignedUrlResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - folderId or fileId required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Folder or file not found")
    })
    @PostMapping("/upload")
    public ResponseEntity<PreSignedUrlResponse> generateUploadUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Pre-signed URL request for upload",
                    required = true
            )
            @RequestBody PreSignedUrlRequest request) {
        User currentUser = userService.getCurrentUser();
        request.setAction("write");
        if (request.getFolderId() == null && request.getFileId() == null) {
            throw new IllegalArgumentException("folderId or fileId is required for upload pre-signing");
        }
        PreSignedUrlResponse response = preSignedUrlService.generatePreSignedUrl(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Generate pre-signed download URL",
            description = "Generates a time-limited, one-time-use token for secure file downloads. Requires fileId."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-signed URL generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreSignedUrlResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - fileId required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @PostMapping("/download")
    public ResponseEntity<PreSignedUrlResponse> generateDownloadUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Pre-signed URL request for download",
                    required = true
            )
            @RequestBody PreSignedUrlRequest request) {
        User currentUser = userService.getCurrentUser();
        request.setAction("read");
        if (request.getFileId() == null) {
            throw new IllegalArgumentException("fileId is required for download pre-signing");
        }
        PreSignedUrlResponse response = preSignedUrlService.generatePreSignedUrl(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Execute pre-signed download",
            description = "Downloads a file using a pre-signed token. Token is invalidated after use. Supports idempotency."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/read")
    public ResponseEntity<ByteArrayResource> executePreSignedDownload(
            @Parameter(description = "Pre-signed token for download", required = true)
            @RequestParam String token,
            @Parameter(description = "Optional idempotency key for retry-safe downloads")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            ByteArrayResource cached = idempotencyService.getCachedResponse(idempotencyKey, ByteArrayResource.class);
            if (cached != null) {
                // Try to validate token to get file info for headers
                // If token is already invalidated (from first request), use default headers
                try {
                    Map<String, Object> tokenData = preSignedUrlService.validateToken(token);
                    Long fileId = tokenData.get("fileId") != null
                            ? ((Number) tokenData.get("fileId")).longValue()
                            : null;
                    Long userId = ((Number) tokenData.get("userId")).longValue();
                    var file = fileService.getFileByIdAndUser(fileId, userId);
                    
                    String contentType = file.getContentType();
                    if (contentType == null) {
                        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    }
                    String downloadName = file.getOriginalName();
                    if (downloadName == null) {
                        downloadName = "downloaded-file";
                    }
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + downloadName + "\"")
                            .body(cached);
                } catch (RuntimeException e) {
                    // Token already invalidated, use default headers for cached response
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"downloaded-file\"")
                            .body(cached);
                }
            }
        }
        
        // Validate token
        Map<String, Object> tokenData = preSignedUrlService.validateToken(token);
        Long fileId = tokenData.get("fileId") != null
                ? ((Number) tokenData.get("fileId")).longValue()
                : null;
        String action = (String) tokenData.get("action");

        if (!"read".equals(action)) {
            throw new RuntimeException("Token is not valid for read operation");
        }

        // Get file and download
        Long userId = ((Number) tokenData.get("userId")).longValue();
        var file = fileService.getFileByIdAndUser(fileId, userId);
        byte[] data = Objects.requireNonNull(fileService.downloadFile(fileId, userId), "File data cannot be null");
        ByteArrayResource resource = new ByteArrayResource(data);

        // Store in idempotency cache if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            idempotencyService.storeResponse(idempotencyKey, resource);
        }

        // Invalidate token after use (one-time use)
        preSignedUrlService.invalidateToken(token);

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String downloadName = file.getOriginalName();
        if (downloadName == null) {
            downloadName = "downloaded-file";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    @Operation(
            summary = "Execute pre-signed upload",
            description = "Uploads a file using a pre-signed token. Token is invalidated after use. Supports idempotency."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or folder mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> executePreSignedUpload(
            @Parameter(description = "Pre-signed token for upload", required = true)
            @RequestParam String token,
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional folder ID (must match token if provided)")
            @RequestParam(value = "folderId", required = false) Long folderId,
            @Parameter(description = "Optional idempotency key for retry-safe uploads")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = idempotencyService.getCachedResponse(idempotencyKey, Map.class);
            if (cached != null) {
                return ResponseEntity.ok(cached);
            }
        }
        
        // Validate token
        Map<String, Object> tokenData = preSignedUrlService.validateToken(token);
        Long fileId = tokenData.get("fileId") != null
                ? ((Number) tokenData.get("fileId")).longValue()
                : null;
        String action = (String) tokenData.get("action");
        Long tokenFolderId = tokenData.get("folderId") != null
                ? ((Number) tokenData.get("folderId")).longValue()
                : null;

        if (!"write".equals(action)) {
            throw new RuntimeException("Token is not valid for write operation");
        }

        // Verify file ID matches (if provided)
        if (fileId != null) {
            // This could be for updating an existing file
            // For now, we'll just upload to the specified folder
        }

        if (tokenFolderId == null) {
            throw new RuntimeException("Token is missing folder information");
        }

        Long targetFolderId = folderId != null ? folderId : tokenFolderId;
        if (!tokenFolderId.equals(targetFolderId)) {
            throw new RuntimeException("Token cannot be used for this folder");
        }

        // Upload file
        Long userId = ((Number) tokenData.get("userId")).longValue();
        var uploadedFile = fileService.uploadFile(file, targetFolderId, userId);

        // Create response map
        Map<String, Object> response = Map.of(
                "id", uploadedFile.getId(),
                "originalName", uploadedFile.getOriginalName(),
                "size", uploadedFile.getSize()
        );

        // Store in idempotency cache if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            idempotencyService.storeResponse(idempotencyKey, response);
        }

        // Invalidate token after use (one-time use)
        preSignedUrlService.invalidateToken(token);

        return ResponseEntity.ok(response);
    }
}
