package com.example.document.controller;

import com.example.document.dto.ApiResponse;
import com.example.document.model.CustomerFile;
import com.example.document.service.CustomerFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for uploading and managing customer files.
 *
 * Accepted file types : JPEG, images (PNG/GIF/BMP/WEBP), MS Word (.doc/.docx),
 *                       PDF, Excel (.xls/.xlsx).
 * Maximum file size   : 100 KB (enforced by Spring multipart config and the service layer).
 *
 * Base path: /api/files
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private CustomerFileService customerFileService;

    // ----------------------------------------------------------------
    // CREATE  -  POST /api/files
    // ----------------------------------------------------------------

    /**
     * Upload a file using a multipart/form-data request.
     * The file is validated, written to disk, and its metadata saved
     * to the CustomerFile table.
     *
     * @param file form-data part named "file"
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerFile>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        CustomerFile saved = customerFileService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "File uploaded successfully", saved));
    }

    // ----------------------------------------------------------------
    // READ  -  GET /api/files  and  GET /api/files/{id}
    // ----------------------------------------------------------------

    /** Retrieve metadata for every stored file. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerFile>>> getAllFiles() {
        List<CustomerFile> files = customerFileService.getAllFiles();
        return ResponseEntity.ok(new ApiResponse<>(true, "Files retrieved", files));
    }

    /** Retrieve metadata for a single file by its id. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerFile>> getFileById(@PathVariable Long id) {
        CustomerFile file = customerFileService.getFileById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "File retrieved", file));
    }

    /** Download the actual file content from disk. */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        CustomerFile metadata = customerFileService.getFileById(id);
        Resource resource = customerFileService.loadFileAsResource(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }

    // ----------------------------------------------------------------
    // UPDATE  -  PUT /api/files/{id}
    // ----------------------------------------------------------------

    /** Replace the content of an existing file with a newly uploaded one. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerFile>> updateFile(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        CustomerFile updated = customerFileService.updateFile(id, file);
        return ResponseEntity.ok(new ApiResponse<>(true, "File updated successfully", updated));
    }

    // ----------------------------------------------------------------
    // DELETE  -  DELETE /api/files/{id}
    // ----------------------------------------------------------------

    /** Delete a file from disk and remove its metadata row. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long id) {
        customerFileService.deleteFile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "File deleted successfully", null));
    }
}
