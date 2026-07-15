package io.jenkins.plugins.akeyless.synced.auth;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.Secret;
import io.akeyless.client.model.Auth;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nullable;

/**
 * Universal Identity authentication with Akeyless.
 */
public class UniversalIdentitySyncedAuthMethod extends SyncedAuthMethod {

    private Secret uidToken;

    @DataBoundConstructor
    public UniversalIdentitySyncedAuthMethod() {}

    public Secret getUidToken() { return uidToken; }

    @DataBoundSetter
    public void setUidToken(Secret uidToken) { this.uidToken = uidToken; }

    @Override
    public Auth buildAuth(@Nullable String accessId) {
        Auth auth = new Auth();
        auth.setAccessId(accessId);
        auth.setAccessType("universal_identity");
        auth.setUidToken(Secret.toString(uidToken));
        return auth;
    }

    @Override
    public boolean isConfigured(@Nullable String accessId) {
        return accessId != null && !accessId.isBlank()
                && uidToken != null && !Secret.toString(uidToken).isBlank();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SyncedAuthMethod> {
        @Override
        public String getDisplayName() { return "Universal Identity"; }
    }
}
