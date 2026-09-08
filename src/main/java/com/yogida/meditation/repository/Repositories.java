package com.yogida.meditation.repository;

import com.yogida.meditation.exception.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Small helpers over Spring Data repositories.
 *
 * <p>A plain static function rather than a shared base class: the CRUD services look alike but a
 * generic {@code AbstractCrudService} would make five short, readable classes harder to follow.
 * What is worth stating once is the rule, not the shape.
 */
public final class Repositories {

    private Repositories() {
    }

    /**
     * Deletes a row, or fails with a 404 if it was not there.
     *
     * <p>{@code deleteById} is a silent no-op for a missing id, so without this check a DELETE of
     * something that does not exist answers 204 and the caller believes it removed something.
     * Three services each spelled this out.
     */
    public static <T> void deleteOrThrow(JpaRepository<T, Long> repository, Long id, String entityName) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException(entityName, id);
        }
        repository.deleteById(id);
    }
}
