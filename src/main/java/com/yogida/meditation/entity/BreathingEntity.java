package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a breathing exercise with an ordered list of phases.
 * Each exercise has a required icon stored as an S3 object.
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "breathing")
public class BreathingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ToString.Exclude
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
    @ToString.Exclude
    @OneToMany(mappedBy = "breathing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<BreathingPhaseEntity> phases = new ArrayList<>();

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
        if (!(o instanceof BreathingEntity other)) {
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
        return 78452;
    }
}
