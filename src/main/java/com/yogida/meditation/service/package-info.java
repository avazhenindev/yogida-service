/**
 * Application services.
 *
 * <p>Two shapes in here look like drift and are not. Both are recorded here because the reasoning
 * previously lived in a one-line comment on a field in a controller, where nobody reading this
 * package would find it.
 *
 * <h2>Why the two facades are not the same kind of thing</h2>
 *
 * <p>{@code MediaFacadeService} wraps {@code MediaService}: the admin write path needs upload,
 * duration probing and rating enrichment composed around plain CRUD.
 *
 * <p>{@code BreathingUserFacadeService} and {@code UserMediaFacadeService} deliberately do NOT
 * wrap their {@code *Service} counterpart — they go to the repository directly. That is the point.
 * {@code BreathingService} is the admin read path and returns every audio URL unconditionally;
 * routing the user path through it would mean a caller who is not entitled receives the URLs and
 * is denied only by what the DTO omits afterwards. The user facades resolve the caller, check
 * entitlement, and presign only what that caller may have.
 *
 * <p>So {@code BreathingService} and {@code MediaService} are not "the service for X" — they are
 * the admin path for X. Unifying the two facade shapes would mean either giving the admin path an
 * entitlement check it does not want, or giving the user path a code path that leaks URLs.
 *
 * <h2>Why the CRUD services are not a base class</h2>
 *
 * <p>{@code SubscriptionService}, {@code MediaCategoryService}, {@code ProfileService},
 * {@code FavouriteService} and {@code AppUserService} all expose findAll/findById/create/update/
 * delete and look nearly identical. They stay five separate classes.
 *
 * <p>They are short and each one differs where it matters: ProfileService and FavouriteService
 * scope every read to the caller, AppUserService.delete also removes an entitlement projection
 * that no foreign key would cascade, MediaCategoryService is plain. An
 * {@code AbstractCrudService<T, ID>} would hide those differences behind template methods and make
 * five readable classes into one abstract class plus five subclasses that must be read together.
 *
 * <p>What genuinely repeats is the rule, not the shape, and that is extracted:
 * {@link com.yogida.meditation.repository.Repositories#deleteOrThrow}.
 */
package com.yogida.meditation.service;
