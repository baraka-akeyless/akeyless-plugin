package io.jenkins.plugins.akeyless.synced.config.scope;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * How Jenkins authenticates to Akeyless: one global identity or per Jenkins user.
 * Rendered with {@code f:dropdownDescriptorSelector} (same as {@link io.jenkins.plugins.akeyless.synced.auth.SyncedAuthMethod}).
 */
public abstract class AuthenticationScopeMode extends AbstractDescribableImpl<AuthenticationScopeMode> {

    public abstract boolean isPerUser();

    public static DescriptorExtensionList<AuthenticationScopeMode, Descriptor<AuthenticationScopeMode>> all() {
        return Jenkins.get().getDescriptorList(AuthenticationScopeMode.class);
    }
}
