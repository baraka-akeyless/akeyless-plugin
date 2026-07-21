package io.jenkins.plugins.akeyless.synced.auth;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.Secret;
import io.akeyless.client.model.Auth;
import io.jenkins.plugins.akeyless.util.AuthInputNormalizer;
import javax.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * JWT authentication with Akeyless ({@code access_type=jwt}).
 * Requires Access ID (gateway auth method) and a JWT issued by the configured OIDC/JWT issuer.
 */
public class JwtSyncedAuthMethod extends SyncedAuthMethod {

    private Secret jwt;

    @DataBoundConstructor
    public JwtSyncedAuthMethod() {}

    public Secret getJwt() {
        return jwt;
    }

    @DataBoundSetter
    public void setJwt(Secret jwt) {
        this.jwt = jwt;
    }

    @Override
    public Auth buildAuth(@Nullable String accessId) {
        Auth auth = new Auth();
        auth.setAccessId(trimCopyPasted(accessId));
        auth.setAccessType("jwt");
        auth.setJwt(AuthInputNormalizer.normalizeJwt(Secret.toString(jwt)));
        return auth;
    }

    @Override
    public boolean isConfigured(@Nullable String accessId) {
        String id = trimCopyPasted(accessId);
        if (id == null || id.isBlank() || jwt == null) {
            return false;
        }
        return !AuthInputNormalizer.normalizeJwt(Secret.toString(jwt)).isBlank();
    }

    private static String trimCopyPasted(@Nullable String s) {
        return s == null ? null : s.trim();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SyncedAuthMethod> {
        @Override
        public String getDisplayName() {
            return "JWT";
        }
    }
}
