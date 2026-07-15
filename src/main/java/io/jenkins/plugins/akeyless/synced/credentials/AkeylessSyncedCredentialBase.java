package io.jenkins.plugins.akeyless.synced.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsUnavailableException;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedAuthResolver;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient;

import javax.annotation.Nullable;

/**
 * Base for Akeyless-backed credentials synced into Jenkins. {@code ownerUserId} is set when authentication scope
 * is per-user so secret fetches use that user's Akeyless identity.
 */
public abstract class AkeylessSyncedCredentialBase extends BaseStandardCredentials {

    @Nullable
    private final String ownerUserId;

    protected AkeylessSyncedCredentialBase(String id, String description, @Nullable String ownerUserId) {
        super(ownerUserId != null ? CredentialsScope.USER : CredentialsScope.GLOBAL, id, description);
        this.ownerUserId = ownerUserId;
    }

    @Nullable
    public String getOwnerUserId() {
        return ownerUserId;
    }

    protected static AkeylessSyncedClient requireClient(@Nullable String ownerUserId) {
        AkeylessSyncedClient client = AkeylessSyncedAuthResolver.resolveClient(ownerUserId);
        if (client == null) {
            throw new CredentialsUnavailableException(
                    ownerUserId != null
                            ? "Akeyless Credentials Provider: user authentication is not configured"
                            : "Akeyless Credentials Provider is not configured");
        }
        return client;
    }

    protected AkeylessSyncedClient requireClient() {
        return requireClient(ownerUserId);
    }
}
