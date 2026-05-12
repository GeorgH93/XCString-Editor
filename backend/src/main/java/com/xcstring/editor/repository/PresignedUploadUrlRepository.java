package com.xcstring.editor.repository;

import com.xcstring.editor.entity.PresignedUploadUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresignedUploadUrlRepository extends JpaRepository<PresignedUploadUrl, Long> {
    @Query("SELECT p FROM PresignedUploadUrl p WHERE p.token = :token AND p.expiresAt > :now")
    Optional<PresignedUploadUrl> findValidByToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    List<PresignedUploadUrl> findByFileIdOrderByCreatedAtDesc(Long fileId);
}
