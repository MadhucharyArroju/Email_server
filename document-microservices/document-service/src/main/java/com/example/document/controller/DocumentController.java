package com.example.document.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.document.model.Document;
import com.example.document.service.DocumentService;
import com.example.document.dto.ApiResponse;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Document>>> getAllDocuments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        List<Document> documents = documentService.getAllDocuments(status, type);
        return ResponseEntity.ok(new ApiResponse<>(true, "Documents retrieved", documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document retrieved", document));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Document>> uploadDocument(@RequestBody Document document) {
        Document uploaded = documentService.uploadDocument(document);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Document uploaded successfully", uploaded));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> updateDocument(
            @PathVariable Long id, @RequestBody Document document) {
        Document updated = documentService.updateDocument(id, document);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document updated successfully", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Document>> updateStatus(
            @PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        Document updated = documentService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Status updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document deleted successfully", null));
    }
}

class StatusUpdateRequest {
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
