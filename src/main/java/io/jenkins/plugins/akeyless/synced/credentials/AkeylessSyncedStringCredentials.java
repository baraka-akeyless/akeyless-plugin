package io.jenkins.plugins.akeyless.synced.credentials;

import com.cloudbees.plugins.credentials.CredentialsUnavailableException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import io.akeyless.client.ApiException;
import io.jenkins.plugins.akeyless.synced.AkeylessSyncedCredentialsProvider;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient.GetSecretValueResult;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AkeylessSyncedStringCredentials extends AkeylessSyncedCredentialBase implements StringCredentials {

    private static final Logger LOG = Logger.getLogger(AkeylessSyncedStringCredentials.class.getName());

    private final String akeylessPath;

    public AkeylessSyncedStringCredentials(String id, String akeylessPath, String description) {
        this(id, akeylessPath, description, null);
    }

    public AkeylessSyncedStringCredentials(String id, String akeylessPath, String description, @Nullable String ownerUserId) {
        super(id, description, ownerUserId);
        this.akeylessPath = akeylessPath != null ? akeylessPath : id;
    }

    @NonNull
    public Secret getSecret() {
        AkeylessSyncedClient client = requireClient();
        try {
            LOG.log(Level.INFO, "Akeyless Credentials Provider: fetching secret value for credential id={0} path={1}", new Object[]{getId(), akeylessPath});
            GetSecretValueResult r = client.getSecretValue(akeylessPath);
            if (r.isString()) {
                return Secret.fromString(r.getStringValue());
            }
            throw new CredentialsUnavailableException("Secret '" + akeylessPath + "' is binary, cannot use as string");
        } catch (ApiException e) {
            LOG.log(Level.WARNING, "Akeyless Credentials Provider: failed to get secret for path={0}: {1}", new Object[]{akeylessPath, e.getMessage()});
            throw new CredentialsUnavailableException("Could not retrieve secret from Akeyless: " + e.getMessage(), e);
        }
    }

    /** @deprecated use {@link AkeylessSyncedCredentialBase#requireClient(String)} */
    @Deprecated
    static AkeylessSyncedClient getClient() {
        return requireClient(null);
    }

    static AkeylessSyncedClient getClient(@Nullable String ownerUserId) {
        return requireClient(ownerUserId);
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        @NonNull
        public String getDisplayName() { return "Akeyless Secret Text"; }

        @Override
        public boolean isApplicable(CredentialsProvider scope) {
            return scope instanceof AkeylessSyncedCredentialsProvider;
        }
    }
}
