package com.xcstring.editor.repository;

import com.xcstring.editor.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findByToken(String token);
    List<Invite> findByCreatedByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Modifying
    @Query("DELETE FROM Invite i WHERE i.expiresAt < :now")
    void deleteExpiredInvites(@Param("now") LocalDateTime now);
}
