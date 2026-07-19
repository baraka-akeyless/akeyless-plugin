package io.jenkins.plugins.akeyless.synced.config.scope;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class PerUserAuthenticationScopeMode extends AuthenticationScopeMode {

    @DataBoundConstructor
    public PerUserAuthenticationScopeMode() {}

    @Override
    public boolean isPerUser() {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AuthenticationScopeMode> {
        @Override
        public String getDisplayName() {
            return "Per Jenkins user";
        }
    }
}
