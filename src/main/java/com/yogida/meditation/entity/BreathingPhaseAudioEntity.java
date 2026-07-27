package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Associates a breathing phase with one audio S3 object.
 * A phase may have many audio records; the mobile client picks one at random per phase transition.
 */
@Data
@Entity
@Table(name = "breathing_phase_audio")
public class BreathingPhaseAudioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private BreathingPhaseEntity phase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audio_object_id", nullable = false)
    private S3ObjectEntity audioObject;
}
