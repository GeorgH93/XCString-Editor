package com.xcstring.editor.repository;

import com.xcstring.editor.entity.XCStringFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XCStringFileRepository extends JpaRepository<XCStringFile, Long> {
    List<XCStringFile> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<XCStringFile> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);
    
    @Query("SELECT DISTINCT f FROM XCStringFile f JOIN FileShare s ON f.id = s.file.id WHERE s.sharedWithUser.id = :userId ORDER BY f.updatedAt DESC")
    List<XCStringFile> findSharedWithUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM XCStringFile f WHERE f.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
