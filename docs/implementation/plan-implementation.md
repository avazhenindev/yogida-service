# Plan Implementation — Media Metadata

## Phase 1: Database Migration
- status: completed

### Files
- `src/main/resources/db/changelog/changes/022-add-media-duration-and-ratings.yaml`
- `src/main/resources/db/changelog/db.changelog-master.yaml`

## Phase 2: Entity, Repository, and Service
- status: completed

### Files
- `src/main/java/com/yogida/meditation/entity/MediaEntity.java` — add `durationSeconds`
- `src/main/java/com/yogida/meditation/entity/MediaRatingEntity.java` — new
- `src/main/java/com/yogida/meditation/dto/MediaRatingSummary.java` — new record
- `src/main/java/com/yogida/meditation/repository/MediaRatingRepository.java` — new
- `src/main/java/com/yogida/meditation/service/api/MediaRatingApi.java` — new interface
- `src/main/java/com/yogida/meditation/service/MediaRatingService.java` — new service

## Phase 3: API Contract and Mapping
- status: completed

### Files
- `src/main/java/com/yogida/meditation/dto/MediaDto.java` — add `durationSeconds`, `averageRating`
- `src/main/java/com/yogida/meditation/dto/MediaCreateRequest.java` — add `durationSeconds`
- `src/main/java/com/yogida/meditation/dto/MediaUpdateRequest.java` — add `durationSeconds`
- `src/main/java/com/yogida/meditation/dto/MediaFileUpdateRequest.java` — add `durationSeconds`
- `src/main/java/com/yogida/meditation/mapper/MediaMapper.java` — map `durationSeconds`, ignore `averageRating`
- `src/main/java/com/yogida/meditation/service/MediaService.java` — propagate `durationSeconds`
- `src/main/java/com/yogida/meditation/service/MediaFacadeService.java` — enrich DTOs with `averageRating`

## Phase 4: Media Premium and Tags Metadata
- status: completed

### Files
- `src/main/resources/db/changelog/changes/023-add-media-premium-and-tags.yaml` — new
- `src/main/java/com/yogida/meditation/entity/TagEntity.java` — new
- `src/main/java/com/yogida/meditation/repository/TagRepository.java` — new
- `src/main/java/com/yogida/meditation/entity/MediaEntity.java` — add `isPremium`, `tags`
- `src/main/java/com/yogida/meditation/dto/MediaDto.java` — add `isPremium`, `tags`
- `src/main/java/com/yogida/meditation/dto/MediaCreateRequest.java` — add `isPremium`, `tagIds`
- `src/main/java/com/yogida/meditation/dto/MediaUpdateRequest.java` — add `isPremium`, `tagIds`
- `src/main/java/com/yogida/meditation/dto/MediaFileUpdateRequest.java` — add `isPremium`, `tagIds`
- `src/main/java/com/yogida/meditation/mapper/MediaMapper.java` — map `isPremium`, `tags`
- `src/main/java/com/yogida/meditation/service/MediaService.java` — handle `isPremium`, `tags`
- `src/main/java/com/yogida/meditation/repository/MediaRepository.java` — EntityGraph for tags

## Phase 5: Automatic Duration Extraction
- status: completed

### Files
- `src/main/java/com/yogida/meditation/service/api/MediaDurationApi.java` — new interface
- `src/main/java/com/yogida/meditation/service/MediaDurationService.java` — ffprobe-based impl
- `src/main/java/com/yogida/meditation/config/MediaDurationProperties.java` — config properties
- `src/main/resources/application.properties` — add duration config
- `src/main/java/com/yogida/meditation/service/MediaFacadeService.java` — extract duration on create/update

## Phase 6: Written Review Text and Listing
- status: completed

### Files
- `src/main/resources/db/changelog/changes/024-add-media-review-table.yaml` — new
- `src/main/java/com/yogida/meditation/entity/MediaReviewEntity.java` — new
- `src/main/java/com/yogida/meditation/repository/MediaReviewRepository.java` — new
- `src/main/java/com/yogida/meditation/service/api/MediaReviewApi.java` — new interface
- `src/main/java/com/yogida/meditation/service/MediaReviewService.java` — new service
- `src/main/java/com/yogida/meditation/dto/MediaRatingRequest.java` — new record
- `src/main/java/com/yogida/meditation/dto/MediaRatingResponse.java` — new record
- `src/main/java/com/yogida/meditation/dto/MediaReviewRequest.java` — new record
- `src/main/java/com/yogida/meditation/dto/MediaReviewResponse.java` — new record
- `src/main/java/com/yogida/meditation/dto/MediaRatingSummaryResponse.java` — new record
- `src/main/java/com/yogida/meditation/controller/api/MediaRatingControllerApi.java` — new interface
- `src/main/java/com/yogida/meditation/controller/MediaRatingController.java` — new controller

## Phase 7: Tests
- status: completed

### Files
- `src/test/java/com/yogida/meditation/service/MediaFacadeServiceTest.java` — extend duration propagation
- `src/test/java/com/yogida/meditation/service/MediaRatingServiceTest.java` — new
- `src/test/java/com/yogida/meditation/service/MediaReviewServiceTest.java` — new
