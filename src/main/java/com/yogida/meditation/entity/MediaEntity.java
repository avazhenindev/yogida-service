package com.yogida.meditation.entity;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;


    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_object_id", nullable = false)
    private S3ObjectEntity mediaObject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "description")
    private String description;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picture_object_id")
    private S3ObjectEntity pictureObject;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private MediaCategoryEntity category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "requires_premium_subscription", nullable = false)
    private boolean requiresPremiumSubscription = false;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @BatchSize(size = 30)
    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "media_tag",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

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
        if (!(o instanceof MediaEntity other)) {
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
        return 24756;
    }
}
