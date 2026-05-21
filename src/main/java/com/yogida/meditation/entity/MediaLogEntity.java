package com.yogida.meditation.entity;

import com.yogida.meditation.enums.MediaLogAction;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit log entry for media lifecycle events: ADDED, REMOVED, UPDATED, ERROR.
 * Logs are cascade-deleted when the parent MediaEntity is deleted.
 */
@Data
@Entity
@Table(name = "media_log")
public class MediaLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", foreignKey = @ForeignKey(name = "fk_media_log_media"))
    private MediaEntity media;

    /** Snapshot of the media name at log time for historical reference after deletion. */
    @Column(name = "media_name", nullable = false)
    private String mediaName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private MediaLogAction action;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

