package com.example.document.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for an uploaded customer file.
 * Mapped to the CustomerFile table in PostgreSQL.
 *
 * Required properties (as per spec): fileId, fileName, fileSize, filePath.
 * uploadedAt is extra metadata kept for auditing/ordering.
 */
@Entity
@Table(name = "CustomerFile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    /** Original name of the uploaded file, e.g. invoice.pdf */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Size of the file in bytes (must be <= 100 KB / 102400 bytes). */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** Absolute path on disk where the file content is stored. */
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    /** Timestamp the file was stored. */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
