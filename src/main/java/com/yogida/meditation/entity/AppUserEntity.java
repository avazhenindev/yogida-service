package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, length = 100)
    private String keycloakUserId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

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
        if (!(o instanceof AppUserEntity other)) {
            return false;
        }
        // A row with no id yet is equal only to itself: two unsaved instances are distinct, and
        // treating them as equal would collapse them inside a Set or make List.remove pick the
        // wrong element off an orphanRemoval collection.
        if (this.userId == null || other.getUserId() == null) {
            return false;
        }
        return this.userId.equals(other.getUserId());
    }

    /**
     * Constant by design. The id is null before persist and assigned after, so a hash derived
     * from it would change while the object sits in a HashSet and the object would become
     * unfindable in the set it is already in.
     */
    @Override
    public int hashCode() {
        return 80844;
    }
}
