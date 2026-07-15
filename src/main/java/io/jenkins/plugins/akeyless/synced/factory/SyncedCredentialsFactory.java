package io.jenkins.plugins.akeyless.synced.factory;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import io.jenkins.plugins.akeyless.synced.credentials.AkeylessSyncedCertificateCredentials;
import io.jenkins.plugins.akeyless.synced.credentials.AkeylessSyncedFileCredentials;
import io.jenkins.plugins.akeyless.synced.credentials.AkeylessSyncedSSHUserPrivateKeyCredentials;
import io.jenkins.plugins.akeyless.synced.credentials.AkeylessSyncedStringCredentials;
import io.jenkins.plugins.akeyless.synced.credentials.AkeylessSyncedUsernamePasswordCredentials;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Creates Jenkins credential instances from Akeyless item metadata (name, description, tags) and client for on-demand fetch.
 */
public final class SyncedCredentialsFactory {

    private SyncedCredentialsFactory() {}

    /**
     * Create a Jenkins credential from an Akeyless item. Credentials do not store the API client
     * so they remain serializable and safe for Jenkins class filter; the client is built on demand
     * when the secret value is fetched.
     * @param id Jenkins credential id (sanitized path, [a-zA-Z0-9_.-]+)
     * @param akeylessPath full Akeyless path for getSecretValue API
     * @param description optional description
     * @param tags tags from Akeyless (e.g. jenkins:credentials:type, jenkins:credentials:username)
     * @return credential if type is supported, empty otherwise
     */
    public static Optional<StandardCredentials> create(
            String id,
            String akeylessPath,
            String description,
            Map<String, String> tags) {
        return create(id, akeylessPath, description, tags, null);
    }

    public static Optional<StandardCredentials> create(
            String id,
            String akeylessPath,
            String description,
            Map<String, String> tags,
            @Nullable String ownerUserId) {
        String type = tags.getOrDefault(SyncedCredentialTags.TYPE, SyncedCredentialType.STRING);
        String username = tags.getOrDefault(SyncedCredentialTags.USERNAME, "");
        String filename = tags.getOrDefault(SyncedCredentialTags.FILENAME, id);
        String valueFormat = tags.getOrDefault(SyncedCredentialTags.VALUE_FORMAT, "").trim();

        switch (type) {
            case SyncedCredentialType.STRING:
                return Optional.of(new AkeylessSyncedStringCredentials(id, akeylessPath, description, ownerUserId));
            case SyncedCredentialType.USERNAME_PASSWORD:
                return Optional.of(new AkeylessSyncedUsernamePasswordCredentials(id, akeylessPath, description, username, valueFormat, ownerUserId));
            case SyncedCredentialType.SSH_USER_PRIVATE_KEY:
                return Optional.of(new AkeylessSyncedSSHUserPrivateKeyCredentials(id, akeylessPath, description, username, valueFormat, ownerUserId));
            case SyncedCredentialType.CERTIFICATE:
                return Optional.of(new AkeylessSyncedCertificateCredentials(id, akeylessPath, description, ownerUserId));
            case SyncedCredentialType.FILE:
                return Optional.of(new AkeylessSyncedFileCredentials(id, akeylessPath, description, filename, ownerUserId));
            default:
                return Optional.empty();
        }
    }
}
