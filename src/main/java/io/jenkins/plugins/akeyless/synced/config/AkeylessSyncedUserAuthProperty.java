package io.jenkins.plugins.akeyless.synced.config;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.akeyless.synced.auth.SyncedAuthMethod;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Per-user Akeyless authentication (access ID + auth method). Shown on the user's Configure page when
 * {@link AuthenticationScope#PER_USER} is selected in global plugin configuration.
 */
public class AkeylessSyncedUserAuthProperty extends UserProperty {

    private String accessId;
    private SyncedAuthMethod authMethod;

    @DataBoundConstructor
    public AkeylessSyncedUserAuthProperty() {}

    public String getAccessId() {
        return accessId;
    }

    @DataBoundSetter
    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public SyncedAuthMethod getAuthMethod() {
        return authMethod;
    }

    @DataBoundSetter
    public void setAuthMethod(SyncedAuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public boolean isConfigured() {
        return authMethod != null && authMethod.isConfigured(accessId);
    }

    @Nullable
    public AkeylessSyncedClient buildClient(String akeylessUrl) {
        if (!isConfigured() || akeylessUrl == null || akeylessUrl.isBlank()) {
            return null;
        }
        return new AkeylessSyncedClient(akeylessUrl.trim(), accessId != null ? accessId.trim() : null, authMethod);
    }

    @Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

        public DescriptorImpl() {
            super(AkeylessSyncedUserAuthProperty.class);
        }

        @Override
        public boolean isEnabled() {
            AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
            return config != null && config.isPerUserAuthentication();
        }

        @Override
        public String getDisplayName() {
            return "Akeyless authentication";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new AkeylessSyncedUserAuthProperty();
        }

        public List<Descriptor<SyncedAuthMethod>> getAuthMethodDescriptors() {
            return SyncedAuthMethod.all().stream()
                    .map(d -> (Descriptor<SyncedAuthMethod>) d)
                    .toList();
        }
    }
}
