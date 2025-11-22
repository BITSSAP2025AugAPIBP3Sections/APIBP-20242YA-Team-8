package com.rip.vaultify.controller;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FileRepository;
import com.rip.vaultify.repository.UserRepository;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/permissions")
@Tag(name = "Permissions", description = "File sharing and permission management operations")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Operation(
            summary = "Share a file with another user",
            description = "Shares a file with another user by username. Only the file owner can share files. Access levels: READ (view only) or WRITE (view and download)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File shared successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"id\": 1, \"access\": \"READ\", \"message\": \"File shared successfully\"}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid access type (must be READ or WRITE)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can share"),
            @ApiResponse(responseCode = "404", description = "File or user not found")
    })
    @PostMapping("/share")
    public ResponseEntity<?> share(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Share request with fileId, username, and access level",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"fileId\": \"1\", \"username\": \"john_doe\", \"access\": \"READ\"}")
                    )
            )
            @RequestBody Map<String, String> body){
        try {
            // Get current authenticated user (the person trying to share)
            User currentUser = userService.getCurrentUser();
            
            Long fileId = Long.valueOf(body.get("fileId"));
            String username = body.get("username");
            String accessStr = body.get("access");

            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            User targetUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            // Validate access type
            Permission.Access access;
            try {
                access = Permission.Access.valueOf(accessStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid access type. Must be READ or WRITE"));
            }
            
            // Share file (this will validate that currentUser is the owner)
            Permission p = permissionService.share(file, targetUser, access, currentUser);

            return ResponseEntity.ok(Map.of(
                    "id", p.getId(),
                    "access", p.getAccess().name(),
                    "message", "File shared successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Get file permissions",
            description = "Retrieves all permissions for a file. Only the file owner can view permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissions retrieved successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can view permissions"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/file/{fileId}")
    public ResponseEntity<?> getFilePermissions(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId) {
        try {
            User currentUser = userService.getCurrentUser();
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            // Only owner can view permissions
            if (!permissionService.isOwner(file, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only the file owner can view permissions"));
            }
            
            return ResponseEntity.ok(permissionService.getFilePermissionsWithUserInfo(file));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Get all shared files",
            description = "Retrieves all files that have been shared with the current user (including pending and accepted shares)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shared files retrieved successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/shared")
    public ResponseEntity<?> getSharedFiles() {
        try {
            User currentUser = userService.getCurrentUser();
            List<Permission> sharedPermissions = permissionService.getSharedFilesForUser(currentUser);
            
            List<Map<String, Object>> sharedFiles = sharedPermissions.stream()
                    .map(p -> {
                        File file = p.getFile();
                        User owner = permissionService.getFileOwner(file).orElse(null);
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("permissionId", p.getId());
                        fileInfo.put("fileId", file.getId());
                        fileInfo.put("fileName", file.getOriginalName());
                        fileInfo.put("fileSize", file.getSize());
                        fileInfo.put("contentType", file.getContentType());
                        fileInfo.put("uploadedAt", file.getUploadedAt().toString());
                        fileInfo.put("access", p.getAccess().name());
                        fileInfo.put("folderId", file.getFolder().getId());
                        fileInfo.put("folderName", file.getFolder().getName());
                        if (owner != null) {
                            fileInfo.put("ownerUsername", owner.getUsername());
                            fileInfo.put("ownerId", owner.getId());
                        }
                        return fileInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(sharedFiles);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Get file owner information",
            description = "Retrieves the owner information for a file. User must have READ permission or be the owner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Owner information retrieved successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/file/{fileId}/owner")
    public ResponseEntity<?> getFileOwner(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId) {
        try {
            User currentUser = userService.getCurrentUser();
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            // Check if user has permission to view this file
            if (!permissionService.isOwner(file, currentUser) && 
                !permissionService.hasReadPermission(file, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to view this file"));
            }
            
            User owner = permissionService.getFileOwner(file)
                    .orElseThrow(() -> new RuntimeException("File owner not found"));
            
            Map<String, Object> ownerInfo = new HashMap<>();
            ownerInfo.put("id", owner.getId());
            ownerInfo.put("username", owner.getUsername());
            
            return ResponseEntity.ok(ownerInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Mark permission as viewed",
            description = "Marks a shared file permission as viewed by the current user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission marked as viewed",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @PostMapping("/viewed/{permissionId}")
    public ResponseEntity<?> markPermissionAsViewed(
            @Parameter(description = "Permission ID", required = true)
            @PathVariable Long permissionId) {
        try {
            User currentUser = userService.getCurrentUser();
            permissionService.markPermissionAsViewed(permissionId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Permission marked as viewed"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Get accepted shared files",
            description = "Retrieves all files that have been shared with the current user and have been accepted."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Accepted shared files retrieved successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/accepted")
    public ResponseEntity<?> getAcceptedSharedFiles() {
        try {
            User currentUser = userService.getCurrentUser();
            List<Permission> sharedPermissions = permissionService.getAcceptedSharedFilesForUser(currentUser);
            
            List<Map<String, Object>> sharedFiles = sharedPermissions.stream()
                    .map(p -> {
                        File file = p.getFile();
                        User owner = permissionService.getFileOwner(file).orElse(null);
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("permissionId", p.getId());
                        fileInfo.put("fileId", file.getId());
                        fileInfo.put("fileName", file.getOriginalName());
                        fileInfo.put("fileSize", file.getSize());
                        fileInfo.put("contentType", file.getContentType());
                        fileInfo.put("uploadedAt", file.getUploadedAt().toString());
                        fileInfo.put("access", p.getAccess().name());
                        fileInfo.put("folderId", file.getFolder().getId());
                        fileInfo.put("folderName", file.getFolder().getName());
                        if (owner != null) {
                            fileInfo.put("ownerUsername", owner.getUsername());
                            fileInfo.put("ownerId", owner.getId());
                        }
                        return fileInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(sharedFiles);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Update file permission",
            description = "Updates the access level of a file permission. Only the file owner can update permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission updated successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid access type (must be READ or WRITE)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can update permissions"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @PutMapping("/{permissionId}")
    public ResponseEntity<?> updatePermission(
            @Parameter(description = "Permission ID to update", required = true)
            @PathVariable Long permissionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Update request with new access level",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"access\": \"WRITE\"}")
                    )
            )
            @RequestBody Map<String, String> body) {
        try {
            User currentUser = userService.getCurrentUser();
            
            String accessStr = body.get("access");
            Permission.Access newAccess;
            try {
                newAccess = Permission.Access.valueOf(accessStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid access type. Must be READ or WRITE"));
            }
            
            Permission updated = permissionService.updatePermission(permissionId, newAccess, currentUser);
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "access", updated.getAccess().name(),
                    "message", "Permission updated successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
            summary = "Revoke file permission",
            description = "Revokes a file permission, removing access for the user. Only the file owner can revoke permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission revoked successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only owner can revoke permissions"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @DeleteMapping("/{permissionId}")
    public ResponseEntity<?> revokePermission(
            @Parameter(description = "Permission ID to revoke", required = true)
            @PathVariable Long permissionId) {
        try {
            User currentUser = userService.getCurrentUser();
            permissionService.revokePermission(permissionId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Permission revoked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
