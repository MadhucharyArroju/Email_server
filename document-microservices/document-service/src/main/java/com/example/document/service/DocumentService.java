package com.example.document.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.document.model.Document;
import com.example.document.repository.DocumentRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {
    @Autowired
    private DocumentRepository documentRepository;

    public List<Document> getAllDocuments(String status, String type) {
        if (status != null && type != null) {
            return documentRepository.findByStatus(status);
        } else if (status != null) {
            return documentRepository.findByStatus(status);
        } else if (type != null) {
            return documentRepository.findByDocumentType(type);
        }
        return documentRepository.findAll();
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
    }

    public Document uploadDocument(Document document) {
        document.setStatus("UPLOADED");
        document.setUploadedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public Document updateDocument(Long id, Document documentDetails) {
        Document document = getDocumentById(id);
        document.setDocumentName(documentDetails.getDocumentName());
        document.setDocumentType(documentDetails.getDocumentType());
        document.setDescription(documentDetails.getDescription());
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public Document updateStatus(Long id, String status) {
        Document document = getDocumentById(id);
        document.setStatus(status);
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}
