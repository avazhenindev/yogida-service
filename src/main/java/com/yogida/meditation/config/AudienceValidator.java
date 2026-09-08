package com.yogida.meditation.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Rejects a token that was not minted for this API.
 *
 * <p>Without an audience check, any token the realm issued for any client would be accepted here —
 * a token obtained for a different client is still correctly signed by the same issuer, so
 * signature and issuer validation alone let it through.
 *
 * <p>The {@code aud} claim is read as either a string or an array because RFC 7519 permits both,
 * and Keycloak emits the array form as soon as a second audience mapper exists.
 *
 * <p>Extracted from a private nested class in {@link SecurityConfig}, which was doing four
 * unrelated jobs; this one is self-contained and independently testable.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object aud = token.getClaim("aud");
        if (aud == null) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Audience claim missing", null
            ));
        }

        boolean isValid = aud instanceof List<?> audList
            ? audList.stream()
                .map(String::valueOf)
                .anyMatch(expectedAudience::equals)
            : expectedAudience.equals(aud.toString());

        if (!isValid) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Invalid audience claim", null
            ));
        }

        return OAuth2TokenValidatorResult.success();
    }
}
