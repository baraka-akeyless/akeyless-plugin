package io.jenkins.plugins.akeyless.synced;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.CredentialsStoreAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.model.ModelObject;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.Permission;
import io.jenkins.plugins.akeyless.synced.config.AkeylessSyncedCredentialsProviderConfig;
import io.jenkins.plugins.akeyless.synced.supplier.SyncedCredentialsSupplier;
import jenkins.model.Jenkins;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

public class AkeylessSyncedCredentialsStore extends CredentialsStore {

    private final AkeylessSyncedCredentialsProvider provider;

    @Nullable
    private final User owner;

    /** One action per store instance so {@link CredentialsStoreAction#isVisible()} identity checks match the bound URL action. */
    private transient CredentialsStoreAction storeAction;

    public AkeylessSyncedCredentialsStore(AkeylessSyncedCredentialsProvider provider, @Nullable User owner) {
        super(AkeylessSyncedCredentialsProvider.class);
        this.provider = provider;
        this.owner = owner;
    }

    @NonNull
    @Override
    public ModelObject getContext() {
        return owner != null ? owner : Jenkins.get();
    }

    @Override
    public boolean hasPermission2(@NonNull Authentication authentication, @NonNull Permission permission) {
        if (!CredentialsProvider.VIEW.equals(permission)) {
            return false;
        }
        if (owner != null) {
            return owner.getACL().hasPermission2(authentication, permission);
        }
        return Jenkins.get().getACL().hasPermission2(authentication, permission);
    }

    @NonNull
    @Override
    public List<Credentials> getCredentials(@NonNull Domain domain) {
        if (!Domain.global().equals(domain)) {
            return Collections.emptyList();
        }
        if (owner != null) {
            if (!owner.hasPermission(CredentialsProvider.VIEW)) {
                return Collections.emptyList();
            }
            AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
            if (config == null) {
                return Collections.emptyList();
            }
            try (ACLContext ignored = ACL.as2(owner.impersonate2())) {
                return List.copyOf(SyncedCredentialsSupplier.get(config, owner));
            }
        }
        if (Jenkins.get().hasPermission(CredentialsProvider.VIEW)) {
            return provider.getCredentialsInItemGroup(Credentials.class, Jenkins.get(), ACL.SYSTEM2, List.of());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean addCredentials(@NonNull Domain domain, @NonNull Credentials credentials) {
        throw new UnsupportedOperationException("Jenkins may not add credentials to Akeyless");
    }

    @Override
    public boolean removeCredentials(@NonNull Domain domain, @NonNull Credentials credentials) {
        throw new UnsupportedOperationException("Jenkins may not remove credentials from Akeyless");
    }

    @Override
    public boolean updateCredentials(@NonNull Domain domain, @NonNull Credentials current, @NonNull Credentials replacement) {
        throw new UnsupportedOperationException("Jenkins may not update credentials in Akeyless");
    }

    @Nullable
    @Override
    public synchronized CredentialsStoreAction getStoreAction() {
        if (storeAction == null) {
            storeAction = new AkeylessCredentialsStoreAction(this);
        }
        return storeAction;
    }

    /** Read-only Akeyless-backed store listing; visibility does not depend on Jenkins “add credential” descriptors. */
    public static class AkeylessCredentialsStoreAction extends CredentialsStoreAction {

        private final AkeylessSyncedCredentialsStore store;

        public AkeylessCredentialsStoreAction(AkeylessSyncedCredentialsStore store) {
            this.store = store;
        }

        @Override
        @NonNull
        public CredentialsStore getStore() {
            return store;
        }

        @Override
        public boolean isVisible() {
            CredentialsProvider p = store.getProvider();
            if (p == null || !p.isEnabled()) {
                return false;
            }
            return store.hasPermission(CredentialsProvider.VIEW);
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getIconClassName() {
            return isVisible() ? "symbol-akeyless plugin-akeyless" : null;
        }

        @Override
        public String getDisplayName() {
            return Messages.AkeylessSyncedCredentialsProvider_DisplayName();
        }
    }
}
