package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * A single phase within a breathing exercise (e.g. inhale, hold, exhale).
 * Each phase carries an ordered list of audio files that the mobile client plays at random.
 */
@Data
@Entity
@Table(name = "breathing_phase")
public class BreathingPhaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breathing_id", nullable = false)
    private BreathingEntity breathing;

    /** Semantic key used by animation logic: inhale, hold1, hold2, exhale, etc. */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** Display label shown in the UI (e.g. "Вдох", "Задержка"). */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BreathingPhaseAudioEntity> audioFiles = new ArrayList<>();
}
