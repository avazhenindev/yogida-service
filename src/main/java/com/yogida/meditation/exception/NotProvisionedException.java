package com.yogida.meditation.exception;

/**
 * The request carries no usable authenticated identity.
 *
 * <p>Exists so the 401 that answers it can be narrow. The handler previously matched
 * {@code IllegalStateException}, which also caught a misconfigured storage bucket — so an
 * operator whose {@code cloudflare.r2.public-picture-base-url} was blank saw "Authenticated user
 * is not provisioned" and went looking at Keycloak.
 */
public class NotProvisionedException extends RuntimeException {

    public NotProvisionedException(String message) {
        super(message);
    }
}
