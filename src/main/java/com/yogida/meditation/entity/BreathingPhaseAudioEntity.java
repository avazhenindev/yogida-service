package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Associates a breathing phase with one audio S3 object.
 * A phase may have many audio records; the mobile client picks one at random per phase transition.
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "breathing_phase_audio")
public class BreathingPhaseAudioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private BreathingPhaseEntity phase;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audio_object_id", nullable = false)
    private S3ObjectEntity audioObject;

    /**
     * Identity is the primary key, never the field values.
     *
     * <p>Lombok's {@code @Data} generated equals across every field including the lazy
     * associations, so comparing two entities silently initialised proxies — or threw
     * LazyInitializationException on a detached one. It also made a persistent object's equality
     * change as its fields did.
     *
     * <p>{@code instanceof} rather than {@code getClass()}: a Hibernate lazy proxy is a
     * subclass, and a getClass comparison declares a proxy unequal to the entity it stands for.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BreathingPhaseAudioEntity other)) {
            return false;
        }
        // A row with no id yet is equal only to itself: two unsaved instances are distinct, and
        // treating them as equal would collapse them inside a Set or make List.remove pick the
        // wrong element off an orphanRemoval collection.
        if (this.id == null || other.getId() == null) {
            return false;
        }
        return this.id.equals(other.getId());
    }

    /**
     * Constant by design. The id is null before persist and assigned after, so a hash derived
     * from it would change while the object sits in a HashSet and the object would become
     * unfindable in the set it is already in.
     */
    @Override
    public int hashCode() {
        return 37110;
    }
}
