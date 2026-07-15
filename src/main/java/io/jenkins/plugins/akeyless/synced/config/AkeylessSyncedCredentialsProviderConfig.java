package io.jenkins.plugins.akeyless.synced.config;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import io.jenkins.plugins.akeyless.synced.auth.SyncedAuthMethod;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient;
import io.jenkins.plugins.akeyless.synced.config.scope.AuthenticationScopeMode;
import io.jenkins.plugins.akeyless.synced.config.scope.GlobalAuthenticationScopeMode;
import io.jenkins.plugins.akeyless.synced.config.scope.PerUserAuthenticationScopeMode;
import io.jenkins.plugins.akeyless.synced.supplier.FolderListingCache;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nullable;
import java.util.List;

@Extension
public class AkeylessSyncedCredentialsProviderConfig extends jenkins.model.GlobalConfiguration {

    public static AkeylessSyncedCredentialsProviderConfig get() {
        return jenkins.model.GlobalConfiguration.all().get(AkeylessSyncedCredentialsProviderConfig.class);
    }

    public AkeylessSyncedCredentialsProviderConfig() {
        load();
    }

    private String akeylessUrl;
    /**
     * Selected scope (same UI control as Authentication Method: {@code f:dropdownDescriptorSelector}).
     */
    private AuthenticationScopeMode authenticationScopeMode;
    /** @deprecated legacy string/enum from older releases; migrated in {@link #getAuthenticationScopeMode()}. */
    @Deprecated
    private String authenticationScope;
    private String accessId;
    private SyncedAuthMethod authMethod;
    /** Folder path: secrets are at folderPath + "/" + secretName. No listing. */
    private String folderPath;
    /** Secret names under the folder (one per line). In the job use credentials('secretName'). */
    private String secretNames;
    /** Full secret paths (one per line). Alternative to folder + names; no listing. */
    private String secretPaths;
    /** @deprecated use folderPath or secretPaths; kept for backward compatibility */
    private String pathPrefix;

    /**
     * When true (default), recursive {@code list-items} results for folder-only discovery are cached
     * ({@link io.jenkins.plugins.akeyless.synced.supplier.FolderListingCache#DEFAULT_CACHE_TTL_SECONDS}
     * seconds, aligned with the AWS provider five-minute cache behavior). When false, each credentials refresh
     * triggers a fresh {@code list-items}.
     */
    private Boolean cache;

    public String getAkeylessUrl() { return akeylessUrl; }

    @DataBoundSetter
    public void setAkeylessUrl(String akeylessUrl) { this.akeylessUrl = akeylessUrl; }

    public AuthenticationScopeMode getAuthenticationScopeMode() {
        if (authenticationScopeMode != null) {
            return authenticationScopeMode;
        }
        if (authenticationScope != null && !authenticationScope.isBlank()) {
            return AuthenticationScope.fromString(authenticationScope).isPerUser()
                    ? new PerUserAuthenticationScopeMode()
                    : new GlobalAuthenticationScopeMode();
        }
        return new GlobalAuthenticationScopeMode();
    }

    @DataBoundSetter
    public void setAuthenticationScopeMode(AuthenticationScopeMode authenticationScopeMode) {
        this.authenticationScopeMode = authenticationScopeMode;
        this.authenticationScope = null;
    }

    public boolean isPerUserAuthentication() {
        return getAuthenticationScopeMode().isPerUser();
    }

    public boolean hasAkeylessUrl() {
        return akeylessUrl != null && !akeylessUrl.isBlank();
    }

    public String getAccessId() { return accessId; }

    @DataBoundSetter
    public void setAccessId(String accessId) { this.accessId = accessId; }

    public SyncedAuthMethod getAuthMethod() { return authMethod; }

    @DataBoundSetter
    public void setAuthMethod(SyncedAuthMethod authMethod) { this.authMethod = authMethod; }

    /** Folder path; when not set, pathPrefix is used (so old config works as folder path). */
    public String getFolderPath() {
        if (folderPath != null && !folderPath.isBlank()) return folderPath;
        if (pathPrefix != null && !pathPrefix.isBlank()) return pathPrefix;
        return folderPath;
    }

    @DataBoundSetter
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getSecretNames() { return secretNames; }

    @DataBoundSetter
    public void setSecretNames(String secretNames) { this.secretNames = secretNames; }

    /** Full secret paths only (one per line). pathPrefix is not used here — it is used as folder path when Folder path is empty. */
    public String getSecretPaths() {
        return secretPaths;
    }

    @DataBoundSetter
    public void setSecretPaths(String secretPaths) { this.secretPaths = secretPaths; }

    /** @deprecated use folderPath or secretPaths */
    public String getPathPrefix() { return pathPrefix; }

    @DataBoundSetter
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }

    public Boolean getCache() {
        return cache;
    }

    @DataBoundSetter
    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    /** @return whether list-items discovery should use the in-memory cache (default {@code true}). */
    public boolean isCache() {
        return cache == null || cache;
    }

    @RequirePOST
    @SuppressWarnings("unused")
    public FormValidation doCheckFolderPath(@QueryParameter String folderPath,
                                            @QueryParameter String pathPrefix) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String fp = folderPath;
        if (fp == null || fp.isBlank()) {
            fp = pathPrefix;
        }
        if (FolderPathRules.isForbiddenRootFolder(fp)) {
            return FormValidation.error(
                    "Folder path cannot be '/' or '//' alone — that would list the entire vault. Use a concrete path "
                            + "(e.g. /CICD/jenkins/secrets).");
        }
        return FormValidation.ok();
    }

    /** True when URL and global auth are set (global authentication scope only). */
    public boolean isConfigured() {
        return hasAkeylessUrl()
                && !isPerUserAuthentication()
                && authMethod != null
                && authMethod.isConfigured(accessId);
    }

    /** True when URL is set and secret discovery inputs are present (auth may be per-user). */
    public boolean isDiscoveryConfigured() {
        if (!hasAkeylessUrl()) {
            return false;
        }
        String folderPathEff = getFolderPath();
        if (folderPathEff == null) {
            folderPathEff = "";
        }
        boolean hasNames = secretNames != null && !secretNames.isBlank();
        boolean hasPaths = secretPaths != null && !secretPaths.isBlank();
        boolean hasFolderAndNames = !folderPathEff.isBlank() && hasNames;
        boolean hasFolderOnly = !folderPathEff.isBlank() && !hasFolderAndNames && !hasPaths;
        return hasFolderAndNames || hasPaths || hasFolderOnly;
    }

    @Nullable
    public AkeylessSyncedClient buildClient() {
        if (!hasAkeylessUrl() || isPerUserAuthentication()) {
            return null;
        }
        if (authMethod == null || !authMethod.isConfigured(accessId)) {
            return null;
        }
        String url = akeylessUrl.trim();
        return new AkeylessSyncedClient(url, accessId != null ? accessId.trim() : null, authMethod);
    }

    public List<Descriptor<SyncedAuthMethod>> getAuthMethodDescriptors() {
        return SyncedAuthMethod.all().stream()
                .map(d -> (Descriptor<SyncedAuthMethod>) d)
                .toList();
    }

    public List<Descriptor<AuthenticationScopeMode>> getAuthenticationScopeDescriptors() {
        return AuthenticationScopeMode.all();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        JSONObject section = json.optJSONObject("akeyless-synced-credentials-provider");
        if (section == null) {
            // Backward-compatible section name from standalone credentials-provider builds
            section = json.optJSONObject("akeyless-credentials-provider");
        }
        req.bindJSON(this, section != null ? section : json);
        String folderPathEff = getFolderPath();
        if (folderPathEff == null) {
            folderPathEff = "";
        }
        boolean hasNames = secretNames != null && !secretNames.isBlank();
        boolean hasPaths = secretPaths != null && !secretPaths.isBlank();
        boolean hasFolderAndNames = !folderPathEff.isBlank() && hasNames;
        boolean hasFolderOnly = !folderPathEff.isBlank() && !hasFolderAndNames && !hasPaths;
        if ((hasFolderOnly || hasFolderAndNames) && FolderPathRules.isForbiddenRootFolder(folderPathEff)) {
            throw new FormException(
                    "Folder path cannot be '/' or '//' alone — that would list the entire vault. "
                            + "Use a concrete folder (e.g. /CICD/jenkins/secrets), or use Secret paths only.",
                    "folderPath");
        }
        FolderListingCache.invalidate();
        save();
        return true;
    }
}
