package io.jenkins.plugins.akeyless.synced.auth;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.Secret;
import io.akeyless.client.model.Auth;
import javax.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Email/password authentication with Akeyless.
 * Does not require an Access ID — uses admin email and password directly.
 */
public class EmailSyncedAuthMethod extends SyncedAuthMethod {

    private String adminEmail;
    private Secret adminPassword;

    @DataBoundConstructor
    public EmailSyncedAuthMethod() {}

    public String getAdminEmail() {
        return adminEmail;
    }

    @DataBoundSetter
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public Secret getAdminPassword() {
        return adminPassword;
    }

    @DataBoundSetter
    public void setAdminPassword(Secret adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Override
    public Auth buildAuth(@Nullable String accessId) {
        Auth auth = new Auth();
        auth.setAdminEmail(adminEmail);
        auth.setAdminPassword(Secret.toString(adminPassword));
        return auth;
    }

    @Override
    public boolean isConfigured(@Nullable String accessId) {
        return adminEmail != null
                && !adminEmail.isBlank()
                && adminPassword != null
                && !Secret.toString(adminPassword).isBlank();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SyncedAuthMethod> {
        @Override
        public String getDisplayName() {
            return "Email";
        }
    }
}
