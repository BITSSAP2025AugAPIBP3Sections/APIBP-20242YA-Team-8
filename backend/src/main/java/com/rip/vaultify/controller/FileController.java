package com.rip.vaultify.controller;

import com.rip.vaultify.dto.FileResponse;
import com.rip.vaultify.model.File;
import com.rip.vaultify.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId) throws IOException {

        File uploadedFile = fileService.uploadFile(file, folderId);
        return ResponseEntity.ok(new FileResponse(uploadedFile));
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileResponse>> getFilesByFolder(@PathVariable Long folderId) {
        List<FileResponse> files = fileService.getFilesByFolder(folderId)
                .stream()
                .map(FileResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(@PathVariable Long id) {
        File file = fileService.getFileById(id);
        return ResponseEntity.ok(new FileResponse(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) throws IOException {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) throws IOException {
        File file = fileService.getFileById(id);
        byte[] data = fileService.downloadFile(id);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }
}