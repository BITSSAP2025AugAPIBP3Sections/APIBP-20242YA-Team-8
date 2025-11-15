package com.rip.vaultify.controller;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FileService;
import com.rip.vaultify.service.FolderService;
import com.rip.vaultify.service.PermissionService;
import com.rip.vaultify.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphQLController {

    private final UserService userService;
    private final FileService fileService;
    private final FolderService folderService;
    private final PermissionService permissionService;

    public GraphQLController(UserService userService, FileService fileService, 
                           FolderService folderService, PermissionService permissionService) {
        this.userService = userService;
        this.fileService = fileService;
        this.folderService = folderService;
        this.permissionService = permissionService;
    }

    // User Queries
    @QueryMapping
    public List<User> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public User currentUser() {
        return userService.getCurrentUser();
    }

    // File Queries
    @QueryMapping
    public List<File> files() {
        return fileService.getAllFiles();
    }

    @QueryMapping
    public File file(@Argument String id) {
        return fileService.getFileById(Long.parseLong(id));
    }

    @QueryMapping
    public List<File> filesByFolder(@Argument String folderId) {
        return fileService.getFilesByFolderId(Long.parseLong(folderId));
    }

    // Folder Queries
    @QueryMapping
    public List<Folder> folders() {
        return folderService.getAllFolders();
    }

    @QueryMapping
    public Folder folder(@Argument String id) {
        return folderService.getFolderById(Long.parseLong(id));
    }

    @QueryMapping
    public List<Folder> rootFolders() {
        User currentUser = userService.getCurrentUser();
        return folderService.getRootFolders(currentUser.getId());
    }

    // Permission Queries
    @QueryMapping
    public List<Permission> permissions() {
        return permissionService.getAllPermissions();
    }

    @QueryMapping
    public List<Permission> filePermissions(@Argument String fileId) {
        return permissionService.getPermissionsByFileId(Long.parseLong(fileId));
    }

    @QueryMapping
    public List<Permission> userPermissions() {
        return permissionService.getCurrentUserPermissions();
    }

    @QueryMapping
    public List<File> sharedFiles() {
        return permissionService.getSharedFilesForCurrentUser();
    }

    // Folder Mutations
    @MutationMapping
    public Folder createFolder(@Argument CreateFolderInput input) {
        Long parentId = input.parentId() != null ? Long.parseLong(input.parentId()) : null;
        return folderService.createFolder(input.name(), parentId);
    }

    @MutationMapping
    public Folder updateFolder(@Argument String id, @Argument String name) {
        return folderService.updateFolder(Long.parseLong(id), name);
    }

    @MutationMapping
    public Boolean deleteFolder(@Argument String id) {
        try {
            folderService.deleteFolder(Long.parseLong(id));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Permission Mutations
    @MutationMapping
    public Permission shareFile(@Argument ShareFileInput input) {
        return permissionService.shareFile(
            Long.parseLong(input.fileId()),
            input.username(),
            Permission.Access.valueOf(input.access())
        );
    }

    @MutationMapping
    public Permission updatePermission(@Argument String id, @Argument UpdatePermissionInput input) {
        return permissionService.updatePermission(
            Long.parseLong(id),
            Permission.Access.valueOf(input.access())
        );
    }

    @MutationMapping
    public Boolean removePermission(@Argument String id) {
        try {
            permissionService.removePermission(Long.parseLong(id));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @MutationMapping
    public Permission markAsViewed(@Argument String permissionId) {
        return permissionService.markAsViewed(Long.parseLong(permissionId));
    }

    // Field Resolvers for complex type relationships
    @SchemaMapping
    public User user(File file) {
        return file.getUser();
    }

    @SchemaMapping
    public Folder folder(File file) {
        return file.getFolder();
    }

    @SchemaMapping
    public User user(Folder folder) {
        return folder.getUser();
    }

    @SchemaMapping
    public Folder parent(Folder folder) {
        return folder.getParent();
    }

    @SchemaMapping
    public List<Folder> children(Folder folder) {
        return folder.getChildren();
    }

    @SchemaMapping
    public List<File> files(Folder folder) {
        return folder.getFiles();
    }

    @SchemaMapping
    public File file(Permission permission) {
        return permission.getFile();
    }

    @SchemaMapping
    public User user(Permission permission) {
        return permission.getUser();
    }

    // Input record classes for GraphQL mutations
    public record CreateFolderInput(String name, String parentId) {}
    public record ShareFileInput(String fileId, String username, String access) {}
    public record UpdatePermissionInput(String access) {}
}
