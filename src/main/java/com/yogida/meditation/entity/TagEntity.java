package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Dictionary entity representing a reusable media tag.
 * Tags are normalized by {@code name} (unique, lowercase).
 */
@Data
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
}
