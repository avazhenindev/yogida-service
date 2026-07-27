package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a breathing exercise with an ordered list of phases.
 * Each exercise has a required icon stored as an S3 object.
 */
@Data
@Entity
@Table(name = "breathing")
public class BreathingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "icon_object_id", nullable = false)
    private S3ObjectEntity iconObject;

    @Column(name = "description")
    private String description;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "cycles", nullable = false)
    private int cycles;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "breathing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<BreathingPhaseEntity> phases = new ArrayList<>();
}
