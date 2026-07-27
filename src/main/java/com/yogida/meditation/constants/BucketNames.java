package com.yogida.meditation.constants;

/**
 * Centralised S3/R2 bucket name constants.
 */
public final class BucketNames {

    /** The top-level public Cloudflare R2 bucket. Media pictures, breathing icons and audio all live here. */
    public static final String PUBLIC = "public";

    /** Legacy alias kept for media picture uploads (bucket is PUBLIC, key prefix is "pictures/"). */
    public static final String PICTURES = "pictures";

    /** Key prefix for breathing exercise icons inside the PUBLIC bucket. */
    public static final String BREATHING_ICONS_PREFIX = "breathing/icons/";

    /** Key prefix for breathing phase audio files inside the PUBLIC bucket. */
    public static final String BREATHING_AUDIO_PREFIX = "breathing/audio/";

    private BucketNames() {}
}

