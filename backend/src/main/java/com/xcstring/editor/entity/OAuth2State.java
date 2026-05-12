package com.xcstring.editor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth2_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2State {
    @Id
    @Column(length = 128)
    private String id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
