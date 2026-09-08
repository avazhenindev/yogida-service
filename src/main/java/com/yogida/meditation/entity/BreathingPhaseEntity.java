package com.yogida.meditation.entity;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A single phase within a breathing exercise (e.g. inhale, hold, exhale).
 * Each phase carries an ordered list of audio files that the mobile client plays at random.
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "breathing_phase")
public class BreathingPhaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
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

    // Batched like BreathingEntity.phases already is. Without it the exercise listing issued
    // one select per phase to load its audio — the single largest contributor to the breathing
    // endpoint's query count, and it is on the mobile hot path.
    @BatchSize(size = 100)
    @ToString.Exclude
    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BreathingPhaseAudioEntity> audioFiles = new ArrayList<>();

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
        if (!(o instanceof BreathingPhaseEntity other)) {
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
        return 26203;
    }
}
