package io.jenkins.plugins.akeyless.synced.client;

import hudson.model.User;
import io.jenkins.plugins.akeyless.synced.config.AkeylessSyncedCredentialsProviderConfig;
import io.jenkins.plugins.akeyless.synced.config.AkeylessSyncedUserAuthProperty;

import javax.annotation.Nullable;

/**
 * Resolves an {@link AkeylessSyncedClient} from global or per-user authentication settings.
 */
public final class AkeylessSyncedAuthResolver {

    private AkeylessSyncedAuthResolver() {}

    /**
     * @param ownerUserId Jenkins user id when credentials are user-scoped; {@code null} for global scope.
     */
    @Nullable
    public static AkeylessSyncedClient resolveClient(@Nullable String ownerUserId) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config == null || !config.hasAkeylessUrl()) {
            return null;
        }
        String url = config.getAkeylessUrl().trim();

        if (config.isPerUserAuthentication()) {
            if (ownerUserId == null || ownerUserId.isBlank()) {
                return null;
            }
            User user = User.getById(ownerUserId, false);
            if (user == null) {
                return null;
            }
            AkeylessSyncedUserAuthProperty auth = user.getProperty(AkeylessSyncedUserAuthProperty.class);
            if (auth == null || !auth.isConfigured()) {
                return null;
            }
            return auth.buildClient(url);
        }

        return config.buildClient();
    }
}
