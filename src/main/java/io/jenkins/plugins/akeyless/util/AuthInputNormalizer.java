package io.jenkins.plugins.akeyless.util;

import javax.annotation.Nullable;

/**
 * Shared helpers for normalizing user-provided auth material (copy/paste whitespace, etc.).
 */
public final class AuthInputNormalizer {

    private AuthInputNormalizer() {}

    /**
     * Trims ends and strips internal whitespace/newlines from a JWT (or similar token) so line-wrapped
     * or copy-pasted values still authenticate.
     */
    public static String normalizeJwt(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", "");
    }
}
