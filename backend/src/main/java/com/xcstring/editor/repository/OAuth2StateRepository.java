package com.xcstring.editor.repository;

import com.xcstring.editor.entity.OAuth2State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OAuth2StateRepository extends JpaRepository<OAuth2State, String> {
    Optional<OAuth2State> findByIdAndProvider(String id, String provider);
    
    @Modifying
    @Query("DELETE FROM OAuth2State s WHERE s.createdAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
