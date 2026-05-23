package com.example.document.repository;

import com.example.document.model.CustomerFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CustomerFile}.
 * Provides full CRUD support out of the box (save, findById, findAll, deleteById).
 */
@Repository
public interface CustomerFileRepository extends JpaRepository<CustomerFile, Long> {

    /** Find files by exact original file name. */
    List<CustomerFile> findByFileName(String fileName);
}
