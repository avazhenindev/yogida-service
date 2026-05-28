# TODO Implementation Overview

## Scope Decision

The TODO implementation is scoped to:

- `meditation-service`
- `yogida-ui`

The admin app has no TODO markers and is excluded from implementation, except for documented downstream impact if duration entry becomes necessary.

## Findings Summary

Total actionable TODO markers found: 20.

Backend:

- Add media duration storage.
- Add user media ratings and expose average rating.

Mobile:

- Consume real media metadata in Explore and Detail.
- Extract Explore list item and filters.
- Move Detail data access into a feature API module.
- Split or clarify media API ownership for Home.
- Replace Home placeholder media data.
- Remove Player route URL placeholder and keep signed URL fetching in the player hook.

Admin:

- No actionable TODO markers found.
- No changes planned in this pass.

## Implementation Order

1. Backend schema and API contract.
2. Backend service and rating aggregation.
3. Backend media enrichment for premium and tags.
4. Mobile `IMedia` contract alignment and direct field usage.
5. Mobile API organization.
6. Mobile screen refactors.
7. Testing and deployment checks.
8. Remove resolved TODO comments.

## Plan Files

- Backend metadata plan: `todo-media-metadata.md`
- Mobile contract plan: `../../yogida-ui/docs/todo-media-contract.md`
- Mobile screen refactor plan: `../../yogida-ui/docs/todo-screen-refactors.md`
- Testing and deployment plan: `../../yogida-ui/docs/todo-testing-deployment.md`

## Key Decisions

- Media responses expose `averageRating`; rating count is not required.
- Unrated media returns `averageRating: 0`.
- Written reviews are planned after rating storage is stable.
- Duration is required for persisted media and exposed as non-null metadata.
- Automatic duration extraction is planned so uploads can populate duration before save.
- Premium should be stored as a simple media boolean and exposed as `isPremium`.
- `tags` should be backend-provided from a separate `TagEntity` many-to-many relationship with media, with `[]` for untagged media.
- Player navigation should pass current media through Redux, not an `s3url` route param.
- Player screen scope is playback only; ratings and written reviews belong to Detail or dedicated feedback flows.
- Shared media types live in `shared/types/media.ts` after renaming the old shared type file.

## Acceptance Summary

The TODO implementation is complete when:

- Backend media responses expose required duration and `averageRating` safely.
- Mobile screens render real metadata with safe fallbacks.
- Explore and Detail no longer contain placeholder stats or unresolved TODO comments.
- Home uses API-backed `IMedia` data instead of an empty placeholder array or a separate projection type.
- Player navigation still uses signed streaming URLs from the backend.
- Backend and mobile validation checks pass or unrelated blockers are documented.
