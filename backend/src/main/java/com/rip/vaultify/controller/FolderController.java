package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FolderRequest;
import com.rip.vaultify.dto.FolderResponse;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FolderService;
import com.rip.vaultify.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "Folders", description = "Folder management operations - create, read, update, and delete folders")
@SecurityRequirement(name = "bearerAuth")
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    public FolderController(FolderService folderService, UserService userService) {
        this.folderService = folderService;
        this.userService = userService;
    }

    @Operation(
            summary = "Create a new folder",
            description = "Creates a new folder. Optionally specify a parent folder ID to create a nested folder structure."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folder created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FolderResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Parent folder not found")
    })
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Folder creation request",
                    required = true
            )
            @RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder folder = folderService.createFolder(request.getName(), request.getParentId(), currentUser);
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @Operation(
            summary = "Get all folders",
            description = "Retrieves all folders owned by the current user. Supports ETag-based caching."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folders retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FolderResponse.class))
            ),
            @ApiResponse(responseCode = "304", description = "Not modified - content unchanged since last request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders(
            @Parameter(description = "ETag value for conditional request")
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

    @Operation(
            summary = "Get folder by ID",
            description = "Retrieves a specific folder by ID. User must own the folder. Supports ETag-based caching."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folder retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FolderResponse.class))
            ),
            @ApiResponse(responseCode = "304", description = "Not modified - content unchanged since last request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "ETag value for conditional request")
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

    @Operation(
            summary = "Rename a folder",
            description = "Renames a folder. Only the folder owner can rename it."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folder renamed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FolderResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can rename"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> renameFolder(
            @Parameter(description = "Folder ID to rename", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Folder rename request with new name",
                    required = true
            )
            @RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder updatedFolder = folderService.renameFolder(id, request.getName(), currentUser.getId());
        return ResponseEntity.ok(new FolderResponse(updatedFolder));
    }

    @Operation(
            summary = "Delete a folder",
            description = "Deletes a folder and all its contents. Only the folder owner can delete it."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Folder deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can delete"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @Parameter(description = "Folder ID to delete", required = true)
            @PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        folderService.deleteFolder(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
