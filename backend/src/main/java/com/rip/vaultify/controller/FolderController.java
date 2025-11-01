package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FolderRequest;
import com.rip.vaultify.dto.FolderResponse;
import com.rip.vaultify.model.Folder;
import com.rip.vaultify.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderRequest request) {
        Folder folder = folderService.createFolder(request.getName(), request.getParentId());
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders() {
        List<FolderResponse> folders = folderService.getAllFolders()
                .stream()
                .map(FolderResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(@PathVariable Long id) {
        Folder folder = folderService.getFolderById(id);
        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> renameFolder(@PathVariable Long id, @RequestBody FolderRequest request) {
        Folder updatedFolder = folderService.renameFolder(id, request.getName());
        return ResponseEntity.ok(new FolderResponse(updatedFolder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
