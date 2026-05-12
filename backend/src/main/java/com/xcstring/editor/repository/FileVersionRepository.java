package com.xcstring.editor.repository;

import com.xcstring.editor.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    @Query("SELECT fv FROM FileVersion fv JOIN FETCH fv.createdByUser WHERE fv.file.id = :fileId ORDER BY fv.versionNumber DESC")
    List<FileVersion> findByFileIdOrderByVersionNumberDesc(@Param("fileId") Long fileId);
    
    Optional<FileVersion> findByFileIdAndVersionNumber(Long fileId, Integer versionNumber);
    
    @Query("SELECT MAX(fv.versionNumber) FROM FileVersion fv WHERE fv.file.id = :fileId")
    Optional<Integer> findMaxVersionByFileId(@Param("fileId") Long fileId);
    
    @Query("SELECT COUNT(fv) FROM FileVersion fv WHERE fv.file.id = :fileId")
    long countByFileId(@Param("fileId") Long fileId);
    
    Optional<FileVersion> findByFileIdAndContentHash(Long fileId, String contentHash);
    
    @Query(value = "SELECT fv.total_versions, fv.first_version_date, fv.latest_version_date, fv.total_size_bytes, fv.unique_contributors FROM " +
           "(SELECT COUNT(*) as total_versions, MIN(created_at) as first_version_date, MAX(created_at) as latest_version_date, " +
           "SUM(size_bytes) as total_size_bytes, COUNT(DISTINCT created_by_user_id) as unique_contributors " +
           "FROM file_versions WHERE file_id = :fileId) fv", nativeQuery = true)
    Object[] getVersionStats(@Param("fileId") Long fileId);
}
