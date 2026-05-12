package com.xcstring.editor.repository;

import com.xcstring.editor.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    @Query("SELECT fs FROM FileShare fs JOIN FETCH fs.sharedWithUser WHERE fs.file.id = :fileId ORDER BY fs.sharedWithUser.name")
    List<FileShare> findByFileIdWithUser(@Param("fileId") Long fileId);
    
    Optional<FileShare> findByFileIdAndSharedWithUserId(Long fileId, Long userId);
    boolean existsByFileIdAndSharedWithUserId(Long fileId, Long userId);
    void deleteByFileIdAndSharedWithUserId(Long fileId, Long userId);
}
