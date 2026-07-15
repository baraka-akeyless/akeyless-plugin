package io.jenkins.plugins.akeyless.synced;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Executor;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.akeyless.synced.config.AkeylessSyncedCredentialsProviderConfig;
import io.jenkins.plugins.akeyless.synced.supplier.SyncedCredentialsSupplier;
import jenkins.model.Jenkins;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Extension
public class AkeylessSyncedCredentialsProvider extends CredentialsProvider {

    private static final Logger LOG = Logger.getLogger(AkeylessSyncedCredentialsProvider.class.getName());

    private static final Set<CredentialsScope> USER_SCOPES = Collections.singleton(CredentialsScope.USER);

    @Override
    public Set<CredentialsScope> getScopes(ModelObject object) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config != null && config.isPerUserAuthentication() && object instanceof User) {
            return USER_SCOPES;
        }
        return super.getScopes(object);
    }

    @NonNull
    @Override
    public <C extends Credentials> List<C> getCredentialsInItemGroup(@NonNull Class<C> type,
                                                                     @CheckForNull ItemGroup itemGroup,
                                                                     @CheckForNull Authentication authentication,
                                                                     @NonNull List<DomainRequirement> domainRequirements) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config == null || !config.isDiscoveryConfigured()) {
            return Collections.emptyList();
        }

        if (authentication == null) {
            authentication = ACL.SYSTEM2;
        }

        if (config.isPerUserAuthentication()) {
            return getPerUserCredentials(type, authentication);
        }
        return getGlobalCredentials(type, authentication);
    }

    private static <C extends Credentials> List<C> getGlobalCredentials(
            Class<C> type, Authentication authentication) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config == null || !config.isConfigured()) {
            LOG.log(Level.FINE, "Akeyless Credentials Provider: global auth not configured");
            return Collections.emptyList();
        }
        if (!ACL.SYSTEM2.equals(authentication)
                && !Jenkins.get().getACL().hasPermission2(authentication, CredentialsProvider.VIEW)) {
            return Collections.emptyList();
        }
        return filterCredentials(type, SyncedCredentialsSupplier.get(config));
    }

    private static <C extends Credentials> List<C> getPerUserCredentials(
            Class<C> type, Authentication authentication) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config == null) {
            return Collections.emptyList();
        }

        User user = User.get2(authentication);
        if (user == null && ACL.SYSTEM2.equals(authentication)) {
            user = resolveUserFromBuildContext();
        }
        if (user == null) {
            return Collections.emptyList();
        }

        User resolvedUser = user;
        boolean needImpersonation = !resolvedUser.equals(User.current());
        if (needImpersonation) {
            try (ACLContext ignored = ACL.as2(resolvedUser.impersonate2())) {
                return filterCredentials(type, SyncedCredentialsSupplier.get(config, resolvedUser));
            }
        }
        return filterCredentials(type, SyncedCredentialsSupplier.get(config, resolvedUser));
    }

    /**
     * When a pipeline runs as SYSTEM but was started by a user, resolve that user from the current {@link Run}.
     */
    @Nullable
    private static User resolveUserFromBuildContext() {
        User user = User.current();
        if (user != null) {
            return user;
        }
        Executor executor = Executor.currentExecutor();
        if (executor == null) {
            return null;
        }
        Queue.Executable executable = executor.getCurrentExecutable();
        if (!(executable instanceof Run<?, ?> run)) {
            return null;
        }
        Cause.UserIdCause cause = run.getCause(Cause.UserIdCause.class);
        if (cause == null) {
            return null;
        }
        return User.getById(cause.getUserId(), false);
    }

    private static <C extends Credentials> List<C> filterCredentials(Class<C> type, Collection<StandardCredentials> all) {
        List<C> filtered = all.stream()
                .filter(c -> type.isAssignableFrom(c.getClass()))
                .map(type::cast)
                .collect(Collectors.toList());
        LOG.log(Level.FINE, "Akeyless Credentials Provider: returning {0} credential(s) for type {1}",
                new Object[]{filtered.size(), type.getSimpleName()});
        return filtered;
    }

    @Override
    public CredentialsStore getStore(ModelObject object) {
        AkeylessSyncedCredentialsProviderConfig config = AkeylessSyncedCredentialsProviderConfig.get();
        if (config == null || !config.isDiscoveryConfigured()) {
            return null;
        }
        if (config.isPerUserAuthentication()) {
            return object instanceof User ? new AkeylessSyncedCredentialsStore(this, (User) object) : null;
        }
        return object == Jenkins.get() ? new AkeylessSyncedCredentialsStore(this, null) : null;
    }

    @Override
    public String getIconClassName() {
        return "symbol-akeyless plugin-akeyless";
    }
}
