package io.jenkins.plugins.akeyless.synced.config;

/**
 * Whether Akeyless API authentication is configured once globally (admin) or per Jenkins user.
 */
public enum AuthenticationScope {
    /** Single auth method on Manage Jenkins → Configure System (current default). */
    GLOBAL("Global (single service account)"),
    /** Each user configures Akeyless auth on their profile; secrets sync with that user's Akeyless permissions. */
    PER_USER("Per Jenkins user");

    private final String displayName;

    AuthenticationScope(String displayName) {
        this.displayName = displayName;
    }

    public static AuthenticationScope fromString(String value) {
        if (value == null || value.isBlank()) {
            return GLOBAL;
        }
        try {
            return AuthenticationScope.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return GLOBAL;
        }
    }

    public boolean isPerUser() {
        return this == PER_USER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
