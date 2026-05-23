package com.example.document.service;

import com.example.document.exception.FileStorageException;
import com.example.document.exception.InvalidFileException;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.model.CustomerFile;
import com.example.document.repository.CustomerFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic for storing customer files on disk and managing their
 * metadata in the CustomerFile table. Handles validation, storage and
 * the full CRUD lifecycle.
 */
@Service
public class CustomerFileService {

    /** Maximum allowed file size: 100 KB. */
    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024L; // 102400 bytes

    /** Content types accepted by the upload API: JPEG, images, MS Word, PDF, Excel. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",                                                              // JPEG / JPG
            "image/png",                                                               // PNG image
            "image/gif",                                                               // GIF image
            "image/bmp",                                                               // BMP image
            "image/webp",                                                              // WEBP image
            "application/pdf",                                                         // PDF
            "application/msword",                                                      // MS Word .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // MS Word .docx
            "application/vnd.ms-excel",                                                // Excel .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"         // Excel .xlsx
    ));

    /** File extensions accepted as a secondary check. */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf", "doc", "docx", "xls", "xlsx"
    ));

    @Autowired
    private CustomerFileRepository customerFileRepository;

    @Value("${file.upload-dir:./uploads/customer-files}")
    private String uploadDir;

    private Path storageLocation;

    /** Create the storage directory on application startup. */
    @PostConstruct
    public void init() {
        try {
            this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new FileStorageException(
                    "Could not create the upload directory: " + uploadDir, ex);
        }
    }

    // ----------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------

    /**
     * Validate and store an uploaded file, then persist its metadata.
     *
     * @param file the multipart file from the request
     * @return the persisted CustomerFile record
     */
    public CustomerFile store(MultipartFile file) {
        validate(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());

        // Use a UUID prefix to avoid name collisions on disk.
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetPath = this.storageLocation.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + originalName, ex);
        }

        CustomerFile customerFile = new CustomerFile();
        customerFile.setFileName(originalName);
        customerFile.setFileSize(file.getSize());
        customerFile.setFilePath(targetPath.toString());
        customerFile.setUploadedAt(LocalDateTime.now());

        return customerFileRepository.save(customerFile);
    }

    // ----------------------------------------------------------------
    // READ
    // ----------------------------------------------------------------

    /** Return metadata for every stored file. */
    public List<CustomerFile> getAllFiles() {
        return customerFileRepository.findAll();
    }

    /** Return metadata for a single file or throw if it does not exist. */
    public CustomerFile getFileById(Long id) {
        return customerFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerFile not found with id: " + id));
    }

    /** Load the actual file content from disk as a downloadable resource. */
    public Resource loadFileAsResource(Long id) {
        CustomerFile customerFile = getFileById(id);
        try {
            Path filePath = Paths.get(customerFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileStorageException(
                    "File content missing on disk for id: " + id);
        } catch (MalformedURLException ex) {
            throw new FileStorageException(
                    "Invalid file path for id: " + id, ex);
        }
    }

    // ----------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------

    /**
     * Replace the content of an existing file. The old file is deleted
     * from disk and the metadata row is updated in place.
     */
    public CustomerFile updateFile(Long id, MultipartFile newFile) {
        CustomerFile existing = getFileById(id);
        validate(newFile);

        // Remove the old file from disk.
        deleteFromDisk(existing.getFilePath());

        String originalName = StringUtils.cleanPath(
                newFile.getOriginalFilename() == null ? "unnamed" : newFile.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetPath = this.storageLocation.resolve(storedName);

        try {
            Files.copy(newFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + originalName, ex);
        }

        existing.setFileName(originalName);
        existing.setFileSize(newFile.getSize());
        existing.setFilePath(targetPath.toString());
        existing.setUploadedAt(LocalDateTime.now());

        return customerFileRepository.save(existing);
    }

    // ----------------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------------

    /** Delete a file from disk and remove its metadata row. */
    public void deleteFile(Long id) {
        CustomerFile customerFile = getFileById(id);
        deleteFromDisk(customerFile.getFilePath());
        customerFileRepository.deleteById(id);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Validate that the upload is non-empty, an allowed type, and within 100 KB. */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is missing or empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException(
                    "File too large (" + file.getSize() + " bytes). Maximum allowed size is 100 KB.");
        }

        String contentType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename());
        boolean typeOk = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean extensionOk = extension != null
                && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());

        if (!typeOk && !extensionOk) {
            throw new InvalidFileException(
                    "Unsupported file type '" + contentType + "'. Allowed types: "
                            + "JPEG, image (PNG/GIF/BMP/WEBP), PDF, MS Word (.doc/.docx), "
                            + "Excel (.xls/.xlsx).");
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0 && dot < fileName.length() - 1)
                ? fileName.substring(dot + 1) : null;
    }

    private void deleteFromDisk(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file from disk: " + path, ex);
        }
    }
}
