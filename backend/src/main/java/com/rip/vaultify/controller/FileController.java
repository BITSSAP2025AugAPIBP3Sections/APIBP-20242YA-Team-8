package com.rip.vaultify.controller;

import com.rip.vaultify.config.LoggingConfig;
import com.rip.vaultify.dto.FileResponse;
import com.rip.vaultify.model.File;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.IdempotencyService;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/files")
@Tag(name = "Files", description = "File management operations - upload, download, preview, and delete files")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final Marker FILE_OPERATION_MARKER = MarkerFactory.getMarker("FILE_OPERATION");
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");
    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("AUDIT");

    private final FileService fileService;
    private final UserService userService;
    private final PermissionService permissionService;
    private final IdempotencyService idempotencyService;

    public FileController(FileService fileService, UserService userService, 
                         PermissionService permissionService, IdempotencyService idempotencyService) {
        this.fileService = fileService;
        this.userService = userService;
        this.permissionService = permissionService;
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Generate ETag from file metadata
     */
    private String generateETag(File file) {
        try {
            String data = file.getId() + "_" + file.getUploadedAt() + "_" + file.getSize();
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
            return "\"" + file.getId() + "_" + file.getUploadedAt().hashCode() + "\"";
        }
    }

    @Operation(
            summary = "Upload a file",
            description = "Uploads a file to the specified folder. Supports idempotency via Idempotency-Key header. Maximum file size is 10MB."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID of the folder to upload the file to", required = true)
            @RequestParam("folderId") Long folderId,
            @Parameter(description = "Optional idempotency key for retry-safe uploads")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        
        // Check idempotency if key provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            FileResponse cached = idempotencyService.getCachedResponse(idempotencyKey, FileResponse.class);
            if (cached != null) {
                return ResponseEntity.ok(cached);
            }
        }
        
        User currentUser = userService.getCurrentUser();
        LoggingConfig.LoggingContext.setUserId(currentUser.getId());
        LoggingConfig.LoggingContext.addContext("action", "file_upload");
        LoggingConfig.LoggingContext.addContext("folderId", folderId.toString());
        LoggingConfig.LoggingContext.addContext("fileName", file.getOriginalFilename());
        LoggingConfig.LoggingContext.addContext("fileSize", String.valueOf(file.getSize()));
        LoggingConfig.LoggingContext.addContext("contentType", file.getContentType());
        
        logger.info(FILE_OPERATION_MARKER, "File upload started - fileName: {}, size: {} bytes, folderId: {}, user: {}", 
                   file.getOriginalFilename(), file.getSize(), folderId, currentUser.getId());
        
        try {
            File uploadedFile = fileService.uploadFile(file, folderId, currentUser.getId());
            LoggingConfig.LoggingContext.addContext("fileId", uploadedFile.getId().toString());
            
            logger.info(AUDIT_MARKER, "File upload successful - fileId: {}, fileName: {}, size: {} bytes, user: {}", 
                       uploadedFile.getId(), uploadedFile.getOriginalName(), uploadedFile.getSize(), currentUser.getId());
            
            FileResponse response = new FileResponse(uploadedFile);
            
            // Store in idempotency cache if key provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                idempotencyService.storeResponse(idempotencyKey, response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error(FILE_OPERATION_MARKER, "File upload failed - fileName: {}, folderId: {}, user: {}, error: {}", 
                        file.getOriginalFilename(), folderId, currentUser.getId(), e.getMessage(), e);
            throw e;
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("folderId");
            LoggingConfig.LoggingContext.removeContext("fileName");
            LoggingConfig.LoggingContext.removeContext("fileSize");
            LoggingConfig.LoggingContext.removeContext("contentType");
            LoggingConfig.LoggingContext.removeContext("fileId");
        }
    }

    @Operation(
            summary = "Get all files in a folder",
            description = "Retrieves all files in the specified folder. Supports ETag-based caching via If-None-Match header."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Files retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponse.class))
            ),
            @ApiResponse(responseCode = "304", description = "Not modified - content unchanged since last request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileResponse>> getFilesByFolder(
            @Parameter(description = "ID of the folder", required = true)
            @PathVariable Long folderId,
            @Parameter(description = "ETag value for conditional request")
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        List<FileResponse> files = fileService.getFilesByFolder(folderId, currentUser.getId());
        
        // Generate ETag for the list
        String etag = generateETagForList(files);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(files);
    }
    
    private String generateETagForList(List<FileResponse> files) {
        try {
            StringBuilder data = new StringBuilder();
            for (FileResponse file : files) {
                data.append(file.getId()).append("_").append(file.getSize());
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
            return "\"" + files.hashCode() + "\"";
        }
    }

    @Operation(
            summary = "Get file by ID",
            description = "Retrieves file metadata by ID. User must have READ permission or be the owner. Supports ETag-based caching."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponse.class))
            ),
            @ApiResponse(responseCode = "304", description = "Not modified - content unchanged since last request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "ETag value for conditional request")
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        User currentUser = userService.getCurrentUser();
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        FileResponse fileResponse = new FileResponse(file, currentUser, permissionService);
        
        String etag = generateETag(file);
        
        // Check if client has cached version
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(fileResponse);
    }

    @Operation(
            summary = "Delete a file",
            description = "Deletes a file. Only the file owner can delete files."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can delete"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "File ID to delete", required = true)
            @PathVariable Long id) throws IOException {
        User currentUser = userService.getCurrentUser();
        fileService.deleteFile(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Preview a file",
            description = "Returns the file content for preview. User must have READ permission. Content is returned inline (not as download)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File preview returned",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{id}/preview")
    public ResponseEntity<ByteArrayResource> previewFile(
            @Parameter(description = "File ID to preview", required = true)
            @PathVariable Long id) throws IOException {
        User currentUser = userService.getCurrentUser();
        // getFileByIdAndUser already checks READ permission
        File file = fileService.getFileByIdAndUser(id, currentUser.getId());
        byte[] data = Objects.requireNonNull(fileService.downloadFile(id, currentUser.getId()), "File data cannot be null");

        ByteArrayResource resource = new ByteArrayResource(data);

        // Preview: no attachment header, allows READ users
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
    
    @Operation(
            summary = "Download a file",
            description = "Downloads a file. User must have WRITE permission or be the owner. Supports idempotency and ETag-based caching."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(responseCode = "304", description = "Not modified - content unchanged since last request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - WRITE permission required"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @Parameter(description = "File ID to download", required = true)
            @PathVariable Long id,
            @Parameter(description = "Optional idempotency key for retry-safe downloads")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "ETag value for conditional request")
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) throws IOException {
        User currentUser = userService.getCurrentUser();
        LoggingConfig.LoggingContext.setUserId(currentUser.getId());
        LoggingConfig.LoggingContext.addContext("action", "file_download");
        LoggingConfig.LoggingContext.addContext("fileId", id.toString());
        
        logger.info(FILE_OPERATION_MARKER, "File download attempt - fileId: {}, user: {}", id, currentUser.getId());
        
        try {
            File file = fileService.getFileByIdAndUser(id, currentUser.getId());
            LoggingConfig.LoggingContext.addContext("fileName", file.getOriginalName());
            LoggingConfig.LoggingContext.addContext("fileSize", String.valueOf(file.getSize()));
            
            // Check if user has WRITE permission (required for download)
            User user = new User();
            user.setId(currentUser.getId());
            if (!permissionService.isOwner(file, user) && !permissionService.hasWritePermission(file, user)) {
                logger.warn(SECURITY_MARKER, "File download denied - insufficient permissions - fileId: {}, fileName: {}, user: {}", 
                           id, file.getOriginalName(), currentUser.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Check idempotency if key provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                ByteArrayResource cached = idempotencyService.getCachedResponse(idempotencyKey, ByteArrayResource.class);
                if (cached != null) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                            .body(cached);
                }
            }
            
            String etag = generateETag(file);
            
            // Check if client has cached version
            if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .build();
            }
            
            byte[] data = Objects.requireNonNull(fileService.downloadFile(id, currentUser.getId()), "File data cannot be null");
            ByteArrayResource resource = new ByteArrayResource(data);
            
            // Store in idempotency cache if key provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                idempotencyService.storeResponse(idempotencyKey, resource);
            }
            
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            MediaType mediaType = MediaType.parseMediaType(contentType);
            String downloadName = Objects.requireNonNullElse(file.getOriginalName(), "downloaded-file");
            
            logger.info(AUDIT_MARKER, "File download successful - fileId: {}, fileName: {}, size: {} bytes, user: {}", 
                       id, file.getOriginalName(), file.getSize(), currentUser.getId());

            return ResponseEntity.ok()
                    .eTag(etag)
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error(FILE_OPERATION_MARKER, "File download failed - fileId: {}, user: {}, error: {}", 
                        id, currentUser.getId(), e.getMessage(), e);
            throw e;
        } finally {
            LoggingConfig.LoggingContext.removeContext("action");
            LoggingConfig.LoggingContext.removeContext("fileId");
            LoggingConfig.LoggingContext.removeContext("fileName");
            LoggingConfig.LoggingContext.removeContext("fileSize");
        }
    }
    
    @Operation(
            summary = "Copy a shared file",
            description = "Copies a shared file to a folder in the current user's account. The user must have READ permission on the source file."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File copied successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - missing fileId or folderId"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "File or folder not found")
    })
    @PostMapping("/copy")
    public ResponseEntity<FileResponse> copySharedFile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Copy request with fileId and folderId",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"fileId\": 1, \"folderId\": 2}")
                    )
            )
            @RequestBody Map<String, Long> body) throws IOException {
        User currentUser = userService.getCurrentUser();
        Long fileId = body.get("fileId");
        Long folderId = body.get("folderId");
        
        if (fileId == null || folderId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        File copiedFile = fileService.copySharedFileToFolder(fileId, folderId, currentUser.getId());
        return ResponseEntity.ok(new FileResponse(copiedFile, currentUser, permissionService));
    }
}
