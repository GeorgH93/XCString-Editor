package com.xcstring.editor.repository;

import com.xcstring.editor.entity.Session;
import com.xcstring.editor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    @Query("SELECT s.user FROM Session s WHERE s.id = :id AND s.expiresAt > :now")
    Optional<User> findValidUserBySessionId(@Param("id") String id, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
