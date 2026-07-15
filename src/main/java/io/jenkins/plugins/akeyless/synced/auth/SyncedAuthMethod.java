package io.jenkins.plugins.akeyless.synced.auth;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import io.akeyless.client.model.Auth;
import jenkins.model.Jenkins;

import javax.annotation.Nullable;

public abstract class SyncedAuthMethod extends AbstractDescribableImpl<SyncedAuthMethod> {

    public abstract Auth buildAuth(@Nullable String accessId) throws Exception;

    public abstract boolean isConfigured(@Nullable String accessId);

    public static DescriptorExtensionList<SyncedAuthMethod, Descriptor<SyncedAuthMethod>> all() {
        return Jenkins.get().getDescriptorList(SyncedAuthMethod.class);
    }
}
