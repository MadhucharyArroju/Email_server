package com.example.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.document.model.Document;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStatus(String status);
    List<Document> findByDocumentType(String documentType);
    List<Document> findByUploadedBy(String uploadedBy);
    Document findByDocumentName(String documentName);
}
