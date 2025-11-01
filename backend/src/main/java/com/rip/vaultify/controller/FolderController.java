package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FolderRequest;
import com.rip.vaultify.dto.FolderResponse;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.model.User;
import com.rip.vaultify.service.FolderService;
import com.rip.vaultify.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    public FolderController(FolderService folderService, UserService userService) {
        this.folderService = folderService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder folder = folderService.createFolder(request.getName(), request.getParentId(), currentUser);
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders() {
        User currentUser = userService.getCurrentUser();
        List<FolderResponse> folders = folderService.getAllFoldersByUser(currentUser.getId())
                .stream()
                .map(FolderResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Folder folder = folderService.getFolderByIdAndUser(id, currentUser.getId());
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> renameFolder(@PathVariable Long id, @RequestBody FolderRequest request) {
        User currentUser = userService.getCurrentUser();
        Folder updatedFolder = folderService.renameFolder(id, request.getName(), currentUser.getId());
        return ResponseEntity.ok(new FolderResponse(updatedFolder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        folderService.deleteFolder(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
