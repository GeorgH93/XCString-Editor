package com.xcstring.editor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"file_id", "shared_with_email"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private XCStringFile file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedByUser;

    @Column(name = "shared_with_email", nullable = false, length = 255)
    private String sharedWithEmail;

    @Column(name = "can_edit")
    private Boolean canEdit = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
