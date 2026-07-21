package io.jenkins.plugins.akeyless.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import io.akeyless.client.model.Auth;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Classic Akeyless JWT authentication ({@code access_type=jwt}) for {@code withAkeyless} / Build Wrapper.
 */
public class AkeylessJwtCredentials extends AbstractAkeylessBaseStandardCredentials implements AkeylessCredential {

    private static final long serialVersionUID = 1L;

    @NonNull
    @SuppressWarnings("lgtm[jenkins/plaintext-storage]")
    private String accessId = "";

    private Secret jwt;

    @DataBoundConstructor
    public AkeylessJwtCredentials(
            @CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description) {
        super(scope, id, description);
    }

    @NonNull
    public String getAccessId() {
        return accessId;
    }

    @DataBoundSetter
    public void setAccessId(String accessId) {
        this.accessId = accessId != null ? accessId.trim() : "";
    }

    public Secret getJwt() {
        return jwt;
    }

    @DataBoundSetter
    public void setJwt(Secret jwt) {
        this.jwt = jwt;
    }

    public Auth getAuth() {
        Auth auth = new Auth();
        auth.setAccessType("jwt");
        auth.setAccessId(accessId);
        auth.setJwt(normalizeJwt(Secret.toString(jwt)));
        return auth;
    }

    @Override
    public CredentialsPayload getCredentialsPayload() {
        CredentialsPayload payload = new CredentialsPayload();
        payload.setAuth(getAuth());
        return payload;
    }

    private static String normalizeJwt(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", "");
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Akeyless JWT";
        }
    }
}
