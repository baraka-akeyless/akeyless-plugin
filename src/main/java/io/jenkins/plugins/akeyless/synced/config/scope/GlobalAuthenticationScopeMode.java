package io.jenkins.plugins.akeyless.synced.config.scope;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class GlobalAuthenticationScopeMode extends AuthenticationScopeMode {

    @DataBoundConstructor
    public GlobalAuthenticationScopeMode() {}

    @Override
    public boolean isPerUser() {
        return false;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AuthenticationScopeMode> {
        @Override
        public String getDisplayName() {
            return "Global (single service account)";
        }
    }
}
