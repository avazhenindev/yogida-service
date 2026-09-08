package com.yogida.meditation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the entity identity contract.
 *
 * <p>Lombok's {@code @Data} generated {@code equals}, {@code hashCode} and {@code toString}
 * across every field, including bidirectional associations. That produced two live defects: an
 * unbounded {@code toString} recursion, and an {@code equals} that read lazy associations —
 * silently initialising proxies when attached, and throwing when detached.
 */
class EntityIdentityTest {

    /**
     * {@code BreathingEntity.phases} points at {@code BreathingPhaseEntity.breathing} and back.
     * With a generated all-fields {@code toString} this overflowed the stack, so any log line
     * interpolating a breathing exercise killed the request. Verified against the old code: it
     * threw {@code StackOverflowError}.
     */
    @Test
    @DisplayName("toString does not recurse through the breathing -> phase cycle")
    void breathingToStringTerminates() {
        BreathingEntity exercise = new BreathingEntity();
        exercise.setId(1L);
        BreathingPhaseEntity phase = new BreathingPhaseEntity();
        phase.setId(2L);
        phase.setBreathing(exercise);
        exercise.getPhases().add(phase);

        assertThatCode(exercise::toString).doesNotThrowAnyException();
        assertThatCode(phase::toString).doesNotThrowAnyException();
    }

    /** The second cycle: phase -> audio -> phase. */
    @Test
    @DisplayName("toString does not recurse through the phase -> audio cycle")
    void phaseAudioToStringTerminates() {
        BreathingPhaseEntity phase = new BreathingPhaseEntity();
        phase.setId(1L);
        BreathingPhaseAudioEntity audio = new BreathingPhaseAudioEntity();
        audio.setId(2L);
        audio.setPhase(phase);
        phase.getAudioFiles().add(audio);

        assertThatCode(phase::toString).doesNotThrowAnyException();
        assertThatCode(audio::toString).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("entities with the same id are equal regardless of their other fields")
    void identityIsThePrimaryKey() {
        MediaEntity a = new MediaEntity();
        a.setId(7L);
        a.setName("before rename");
        MediaEntity b = new MediaEntity();
        b.setId(7L);
        b.setName("after rename");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    /**
     * Two unsaved instances must not be equal. If they were, a {@code Set} would collapse them
     * and {@code List.remove} would take the wrong element off an orphan-removal collection.
     */
    @Test
    @DisplayName("unsaved entities are equal only to themselves")
    void unsavedEntitiesAreDistinct() {
        MediaEntity a = new MediaEntity();
        MediaEntity b = new MediaEntity();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isEqualTo(a);
        assertThat(new HashSet<>(List.of(a, b))).hasSize(2);
    }

    /**
     * The behaviour {@code BreathingService} depends on when a phase is removed from an
     * orphan-removal collection: {@code List.remove(Object)} dispatches to {@code equals}, so
     * removing must take the phase with the matching id and nothing else.
     */
    @Test
    @DisplayName("removing from an orphan-removal collection matches by id")
    void listRemoveMatchesById() {
        BreathingPhaseEntity first = new BreathingPhaseEntity();
        first.setId(1L);
        BreathingPhaseEntity second = new BreathingPhaseEntity();
        second.setId(2L);
        List<BreathingPhaseEntity> phases = new ArrayList<>(List.of(first, second));

        BreathingPhaseEntity detachedCopyOfFirst = new BreathingPhaseEntity();
        detachedCopyOfFirst.setId(1L);

        assertThat(phases.remove(detachedCopyOfFirst)).isTrue();
        assertThat(phases).containsExactly(second);
    }

    /**
     * Tags go into a {@code HashSet} while media is assembled, so their hash must not change as
     * they are populated — an id-derived hash would make an entity unfindable in the set it is
     * already in once the id is assigned.
     */
    @Test
    @DisplayName("hashCode is stable across an id being assigned")
    void hashCodeSurvivesPersist() {
        TagEntity tag = new TagEntity();
        int beforePersist = tag.hashCode();
        Set<TagEntity> tags = new HashSet<>();
        tags.add(tag);

        tag.setId(42L);

        assertThat(tag.hashCode()).isEqualTo(beforePersist);
        assertThat(tags).contains(tag);
    }
}
