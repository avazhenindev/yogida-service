package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Dictionary entity representing a reusable media category.
 */
@Data
@Entity
@Table(name = "media_category")
public class MediaCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description")
    private String description;
}

