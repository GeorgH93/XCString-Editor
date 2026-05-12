package com.xcstring.editor.repository;

import com.xcstring.editor.entity.PendingShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingShareRepository extends JpaRepository<PendingShare, Long> {
    List<PendingShare> findByFileIdOrderByCreatedAtDesc(Long fileId);
    List<PendingShare> findBySharedWithEmail(String email);
    Optional<PendingShare> findByFileIdAndSharedWithEmail(Long fileId, String email);
    void deleteBySharedWithEmail(String email);
}
