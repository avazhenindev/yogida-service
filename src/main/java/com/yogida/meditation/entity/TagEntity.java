package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Dictionary entity representing a reusable media tag.
 * Tags are normalized by {@code name} (unique, lowercase).
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "tag")
public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalized unique tag identifier (e.g., "sleep", "focus"). */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** Optional human-readable label (e.g., "Sleep", "Focus"). */
    @Column(name = "display_label", length = 100)
    private String displayLabel;

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
        if (!(o instanceof TagEntity other)) {
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
        return 19478;
    }
}
