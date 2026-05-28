# Media Metadata TODO Implementation Plan

## Scope

This plan covers backend TODOs in the meditation service related to media duration and media ratings. It intentionally does not include admin UI changes. Mobile consumption is covered in the `yogida-ui` plan files.

## Source TODOs

- `src/main/java/com/yogida/meditation/entity/MediaEntity.java`: add an integer duration column for audio or video length.
- `src/main/java/com/yogida/meditation/entity/MediaEntity.java`: add derived average rating and a separate table for individual user ratings.

## Goals

- Store required media duration in the database and expose it through the media API.
- Store one rating per user per media item.
- Allow each user to update their existing rating for a media item.
- Calculate average media rating from all user ratings for that media item.
- Expose average rating with media list/detail responses.
- Avoid exposing JPA entities directly in REST responses.
- Keep all read and write service methods inside explicit transaction boundaries.

## Non-Goals

- No admin UI form/table updates in this scope.
- No change to the existing signed streaming URL flow.

## Timeline

| Phase | Objective | Estimate |
| --- | --- | --- |
| 1 | Add Liquibase migration for duration and ratings | 3-4 hours |
| 2 | Add entity, repository, and service support | 4-6 hours |
| 3 | Extend DTO/API contract and mapper integration | 3-5 hours |
| 4 | Define media premium and tags metadata | 4-7 hours |
| 5 | Add automatic duration extraction | 4-6 hours |
| 6 | Add written review text/listing support | 5-8 hours |
| 7 | Add focused tests and cleanup resolved TODOs | 3-5 hours |

## Phase 1: Database Migration

Create `src/main/resources/db/changelog/changes/022-add-media-duration-and-ratings.yaml` and include it from `src/main/resources/db/changelog/db.changelog-master.yaml`.

Changes:

1. Add required `duration_seconds` to `media`.
2. Create `media_rating` with:
   - `id` as auto-increment primary key.
   - `user_id` referencing `app_user.user_id`.
   - `media_id` referencing `media.id`.
   - `rating` as integer.
   - `created_at` and `updated_at` timestamps.
3. Add unique constraint on `(user_id, media_id)`.
4. Add indexes for aggregate lookups by `media_id` and user lookup by `user_id` if the table grows.
5. Add rating bounds check for `1 <= rating <= 5` if supported consistently by the existing Liquibase/PostgreSQL style.
6. Add rollback for table creation and the new media column.

Migration strategy for existing rows:

1. Add `duration_seconds` as nullable temporarily.
2. Backfill duration for every existing media object before enforcing the constraint.
3. Add a check constraint that duration is greater than zero.
4. Add `NOT NULL` after the backfill succeeds.
5. Fail the migration or deployment if any existing media row cannot be assigned a valid duration.

Acceptance criteria:

- Liquibase migration is included in the master changelog.
- Migration safely handles existing rows through a mandatory backfill before enforcing constraints.
- Existing media cannot keep `duration_seconds = null` after migration.
- New media cannot be created without a valid duration.
- Rollback is present and readable.

## Phase 2: Entity, Repository, and Service

Add or update these backend classes:

- `src/main/java/com/yogida/meditation/entity/MediaEntity.java`
- `src/main/java/com/yogida/meditation/entity/MediaRatingEntity.java`
- `src/main/java/com/yogida/meditation/repository/MediaRatingRepository.java`
- `src/main/java/com/yogida/meditation/service/MediaRatingService.java`
- `src/main/java/com/yogida/meditation/service/api/MediaRatingApi.java` if following the current interface-driven service pattern.

Implementation guidelines:

1. Add `durationSeconds` to `MediaEntity` with `@Column(name = "duration_seconds", nullable = false)`.
2. Prefer DTO-level aggregate fields for rating data unless a transient entity field clearly simplifies mapping.
3. Model `MediaRatingEntity` with `@ManyToOne(fetch = FetchType.LAZY)` relationships to `AppUserEntity` and `MediaEntity`.
4. Treat rating writes as upserts: one row per `(user_id, media_id)`, with later writes updating the existing rating.
5. Keep service methods annotated with `@Transactional` or `@Transactional(readOnly = true)`.
6. Use repository aggregate queries for average/count rather than calculating from loaded collections.
7. Avoid N+1 queries when hydrating media lists; prefer grouped aggregate queries by media id.

Recommended aggregate shape:

- `mediaId`
- `averageRating`

Acceptance criteria:

- Duration is present after create/update, either from automatic extraction or a validated explicit value.
- Rating upsert creates or updates the current user's rating for a media item.
- Average rating is calculated from all ratings for the media item.
- Average rating returns 0 consistently for unrated media.

## Phase 3: API Contract and Mapping

Update DTOs and service flows:

- `src/main/java/com/yogida/meditation/dto/MediaDto.java`
- `src/main/java/com/yogida/meditation/dto/MediaCreateRequest.java`
- `src/main/java/com/yogida/meditation/dto/MediaUpdateRequest.java`
- `src/main/java/com/yogida/meditation/dto/MediaFileUpdateRequest.java`
- `src/main/java/com/yogida/meditation/mapper/MediaMapper.java`
- `src/main/java/com/yogida/meditation/service/MediaService.java`
- `src/main/java/com/yogida/meditation/service/MediaFacadeService.java`

Recommended `MediaDto` additions:

- `Integer durationSeconds`, required and non-null in successful media responses
- `Double averageRating`, with `0` for unrated media
- `Boolean isPremium`, backed by a simple persisted boolean field
- `List<String> tags`, backed by a separate `TagEntity` many-to-many relationship with media

Guidelines:

1. Do not preserve backward compatibility if the new contract needs required duration or changed response semantics.
2. Make duration required for persisted media. Requests may omit it only when the backend can extract it from the uploaded media file before saving.
3. Persist `isPremium` as a simple boolean field on media and expose it directly in DTOs.
4. Implement tags as first-class backend metadata through a separate `TagEntity` and a many-to-many relationship with media.
5. Ensure MapStruct does not trigger lazy loading outside a transaction.

Acceptance criteria:

- `GET /media` returns duration and `averageRating` for every item.
- `GET /media/{id}` returns the same metadata.
- Clients must update to the new media contract before deployment.
- OpenAPI reflects the new DTO/request fields.

## Phase 4: Media Premium and Tags Metadata

Add explicit backend semantics for `isPremium` and `tags` so mobile receives API-ready media metadata and does not derive these fields locally.

### `isPremium`

Source of truth:

- `isPremium` is a simple boolean field on the media record.
- The stored value is the source of truth for API responses.
- No derivation rule is needed for this field.

Implementation steps:

1. Add an `is_premium` boolean column to `media`, with a non-null default such as `false` for existing rows.
2. Add `isPremium` to `MediaEntity` with a non-null boolean mapping.
3. Add `isPremium` to `MediaCreateRequest`, `MediaUpdateRequest`, and `MediaFileUpdateRequest` if those flows can set media metadata.
4. Map the stored field to `MediaDto.isPremium` in list and detail responses.
5. Validate request handling so omitted values use the intended default and explicit values are preserved.
6. Keep premium display semantics independent from subscription mapping logic unless a later product decision changes the model.

Acceptance criteria:

- Media with `is_premium = true` returns `isPremium: true`.
- Media with `is_premium = false` returns `isPremium: false`.
- The media list does not need subscription joins to determine premium state.
- The OpenAPI schema exposes `isPremium` as a non-null boolean.

### `tags`

Source of truth:

- Tags are real backend metadata.
- `TagEntity` is a separate entity.
- Media and tags have a many-to-many relationship.
- Tags are not derived from category.

Recommended implementation:

1. Add `TagEntity` with a unique normalized name and optional display label if tags are user-facing.
2. Add a `Set<TagEntity> tags` relationship to `MediaEntity` with `@ManyToMany` and a `media_tag` join table.
3. Use a join table with `media_id` and `tag_id`, foreign keys, and a unique constraint on `(media_id, tag_id)`.
4. Add Liquibase changes for `tag` and `media_tag`, including rollback.
5. Add `TagRepository` and, if tag creation/update needs business rules, a `TagService`.
6. Add `List<String> tags` to `MediaDto` and map from `TagEntity` names or display labels.
7. Add create/update request support for tag assignment if backend/API clients should manage tags in the same media flow.
8. Fetch tags with media list/detail without N+1 behavior, using entity graphs or targeted repository queries.
9. Return an empty list, not null, for media without tags.

Acceptance criteria:

- `MediaDto.tags` is always a list.
- Media with no tags returns `tags: []`.
- Media list/detail responses include tags without N+1 queries.
- Tags are persisted through `TagEntity` and the `media_tag` many-to-many join table.

## Phase 5: Automatic Duration Extraction

Add backend duration extraction so duration is populated by the service instead of relying only on client input.

Recommended classes:

- `src/main/java/com/yogida/meditation/service/MediaDurationService.java`
- `src/main/java/com/yogida/meditation/service/api/MediaDurationApi.java`
- `src/main/java/com/yogida/meditation/config/MediaDurationProperties.java` if external tools or limits need configuration.

Implementation options:

1. Prefer a proven media metadata tool such as `ffprobe` when the deployment environment can provide it.
2. If external binaries are not acceptable, choose a declared Java media metadata dependency after reviewing license, maintenance status, supported formats, and container size impact.
3. For create/update with a new file, extract duration before the media row is persisted.
4. For existing media, run a one-time backfill job or script that downloads/streams each media object metadata, extracts duration, and writes `duration_seconds` before the `NOT NULL` constraint is enforced.
5. Fail media creation when duration cannot be extracted and no validated duration is provided.
6. Store duration in whole seconds and require a value greater than zero.

Acceptance criteria:

- New media uploads get a valid duration automatically.
- Media update with a new file recalculates duration.
- Media update without a new file keeps the existing duration unless an explicit, validated duration override is allowed.
- Existing rows are backfilled before the migration enforces `NOT NULL`.
- Unsupported or unreadable files fail with a clear validation error.

## Phase 6: Written Review Text and Listing

Add written review support as a follow-up feature after rating storage is stable.

Recommended contract:

- A user may have one rating per media item.
- A user may have one written review per media item.
- Written reviews are stored in a separate `media_review` table.
- Average rating remains calculated from numeric ratings only.
- Review listing returns written reviews with user display metadata, text, timestamps, and rating data only if rating data is joined from the rating table.

Implementation steps:

1. Add a new migration for review text support after the base rating migration is stable.
2. Create a separate `media_review` table with:
   - `id` as auto-increment primary key.
   - `user_id` referencing `app_user.user_id`.
   - `media_id` referencing `media.id`.
   - `review_text` as required text with a length limit.
   - `created_at` and `updated_at` timestamps.
3. Add a unique constraint on `(user_id, media_id)` so one review per user/media is allowed.
4. Add indexes for listing reviews by `media_id` and finding the current user's review.
5. Add `MediaReviewEntity`, `MediaReviewRepository`, `MediaReviewService`, and a service API interface if following the current interface-driven service pattern.
6. Add request/response DTOs such as `MediaRatingRequest`, `MediaRatingResponse`, `MediaReviewRequest`, `MediaReviewResponse`, and `MediaRatingSummaryResponse`.
7. Add endpoints such as:
   - `PUT /media/{mediaId}/rating` for rating upsert.
   - `GET /media/{mediaId}/rating-summary` for aggregate rating data if not embedded in `MediaDto`.
   - `PUT /media/{mediaId}/review` for written review upsert.
   - `GET /media/{mediaId}/reviews` for paginated written reviews.
8. Keep review listing paginated from the start.
9. Add validation for review text length and blank text handling.
10. Keep review text out of `media_rating`; rating and review tables have separate persistence lifecycles.

Acceptance criteria:

- A user can create and update one written review per media item.
- Review text is persisted only in `media_review`, not in `media_rating`.
- Review list is paginated and does not load all reviews at once.
- `averageRating` remains correct after review create/update because review text is separate from numeric ratings.
- Empty review list returns an empty page/list rather than an error.

## Phase 7: Tests

Add focused tests around the actual risk areas.

Backend tests:

1. Extend `src/test/java/com/yogida/meditation/service/MediaFacadeServiceTest.java` to verify duration propagation if requests accept duration.
2. Add `MediaRatingServiceTest` for:
   - first rating insert
   - rating update for the same user/media
   - average calculation
   - zero ratings
   - invalid rating bounds
   - missing media/user handling
3. Add `MediaReviewServiceTest` for:
   - first review insert
   - review update for the same user/media
   - unique one-review-per-user/media behavior
   - blank or over-limit review text rejection
   - missing media/user handling
   - paginated review listing
4. Add a mapper/service test to confirm media list/detail DTOs include `averageRating` and premium flag.
5. Add metadata tests for:
   - `isPremium` true when the media boolean field is true
   - `isPremium` false when the media boolean field is false
   - `tags: []` for media without tags
   - populated tag list for tagged media when tag persistence is implemented
   - many-to-many tag persistence and uniqueness of `(media_id, tag_id)`
6. Add duration extraction tests for valid audio/video files, unsupported files, and unreadable files.
7. If controller endpoints are added, add MockMvc coverage for success and validation failures.

Validation commands:

```bash
cd meditation-service
./mvnw test
```

Optional migration smoke check:

```bash
cd meditation-service
./mvnw spring-boot:run
```

Then confirm Liquibase applies the new changelog against the configured local database.

## Deployment Notes

- Deploy database migration and backend API before mobile changes that display new fields.
- Coordinate backend and mobile deployment because backward compatibility is not required.
- Existing media must be backfilled with valid duration before enforcing `NOT NULL`.
- Existing media will have no ratings until users submit ratings.
- If duration must be entered in the admin UI, schedule a follow-up for `yogida-admin-ui` OpenAPI regeneration and form/table updates.

## Completion Checklist

- [ ] Changelog `022-add-media-duration-and-ratings.yaml` created and included.
- [ ] Existing media duration backfill is planned and executable.
- [ ] `MediaEntity` includes non-null duration.
- [ ] Media create/update rejects missing or invalid duration.
- [ ] Automatic duration extraction is implemented or explicitly configured.
- [ ] Rating entity/repository/service implemented.
- [ ] Rating upsert updates existing user/media rating rows.
- [ ] Average rating is calculated from all user ratings.
- [ ] Written review text/listing support is planned and implemented when scheduled.
- [ ] Media DTO contains mobile-facing metadata.
- [ ] Media service hydrates `averageRating` without N+1 behavior.
- [ ] Premium flag is persisted as a simple media boolean and exposed directly.
- [ ] Tags are implemented through `TagEntity` and a many-to-many relationship with media.
- [ ] Focused tests added.
- [ ] `./mvnw test` passes.
- [ ] Resolved backend TODO comments removed.
