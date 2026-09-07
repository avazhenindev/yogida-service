package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    /**
     * Scheduler path: only the media object is needed for the S3 health check, so the graph
     * stays narrow on purpose.
     */
    @EntityGraph(attributePaths = "mediaObject")
    List<MediaEntity> findAllWithMediaObjectBy();

    /**
     * Admin listing. The graph covers all three lazy {@code @ManyToOne} associations the mapper
     * reads; without it an admin list of N items issued 3N extra selects.
     *
     * <p>The ordering is explicit and is not decoration. Neither this nor the previous
     * {@code findAll()} declared one, so row order was whatever the plan happened to produce —
     * and adding join fetches changes the plan. The admin table renders rows positionally, so an
     * unordered query would silently reshuffle when the graph widened.
     *
     * <p>{@code tags} is deliberately absent: it is a collection already covered by
     * {@code @BatchSize(30)}, and fetching it here would multiply root rows.
     */
    @EntityGraph(attributePaths = {"mediaObject", "pictureObject", "category"})
    List<MediaEntity> findAllByOrderByIdAsc();

    /**
     * User-facing listing, same graph and same reasoning as above — the mobile home screen also
     * consumes this positionally.
     */
    @EntityGraph(attributePaths = {"mediaObject", "pictureObject", "category"})
    List<MediaEntity> findAllByStatusEqualsOrderByIdAsc(MediaStatus status);

    /**
     * Resolves the media owning an S3 object.
     *
     * <p>Replaces a {@code findAll().stream().filter(...)} that scanned the whole table and
     * initialised one {@code s3_object} proxy per row, on every single playback request.
     *
     * <p>{@code findFirstBy} rather than {@code findBy} on purpose: {@code media.media_object_id}
     * carries no unique constraint, so several rows may point at one object. A plain
     * {@code findBy} would throw where the scan silently took the first match.
     */
    Optional<MediaEntity> findFirstByMediaObjectObjectUri(String objectUri);
}
