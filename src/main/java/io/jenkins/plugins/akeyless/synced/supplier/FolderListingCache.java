package io.jenkins.plugins.akeyless.synced.supplier;

import io.akeyless.client.ApiException;
import io.jenkins.plugins.akeyless.synced.client.AkeylessSyncedClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caches recursive {@code list-items} results for folder-only discovery to avoid hammering Akeyless on every
 * {@code CredentialsProvider#getCredentials} call.
 */
public final class FolderListingCache {

    /** When caching is enabled, list-items results are reused for this many seconds (not user-configurable). */
    /** Align with AWS Secrets Manager Credentials Provider cache wording/duration (5 minutes). */
    public static final int DEFAULT_CACHE_TTL_SECONDS = 300;

    private static final Logger LOG = Logger.getLogger(FolderListingCache.class.getName());

    private static final Object LOCK = new Object();

    @GuardedBy("LOCK")
    private static volatile String cachedFolderKey;

    @GuardedBy("LOCK")
    private static volatile int cachedTtlSeconds;

    @GuardedBy("LOCK")
    private static volatile long cachedAtNanos;

    @GuardedBy("LOCK")
    private static volatile List<String> cachedPaths = Collections.emptyList();

    private FolderListingCache() {}

    /** Clears the cache (used when config changes materially). */
    public static void invalidate() {
        synchronized (LOCK) {
            cachedFolderKey = null;
            cachedTtlSeconds = 0;
            cachedAtNanos = 0;
            cachedPaths = Collections.emptyList();
        }
    }

    /**
     * Loads folder contents, using the in-memory cache when {@code cacheEnabled} is true.
     */
    @Nonnull
    public static List<String> getOrLoad(
            @Nonnull AkeylessSyncedClient client,
            @Nonnull String folderNormalized,
            boolean cacheEnabled,
            @Nullable String ownerUserId) throws ApiException {
        if (!cacheEnabled) {
            return client.listSecretItemPathsRecursive(folderNormalized);
        }

        int ttlSec = DEFAULT_CACHE_TTL_SECONDS;
        long ttlNanos = ttlSec * 1_000_000_000L;
        String cacheKey = cacheKey(ownerUserId, folderNormalized);
        synchronized (LOCK) {
            long age = System.nanoTime() - cachedAtNanos;
            if (cacheKey.equals(cachedFolderKey)
                    && ttlSec == cachedTtlSeconds
                    && cachedAtNanos != 0
                    && age >= 0
                    && age < ttlNanos) {
                LOG.log(Level.FINE, "Akeyless Credentials Provider: list-items cache hit for folder={0} (TTL {1}s)",
                        new Object[]{folderNormalized, ttlSec});
                return new ArrayList<>(cachedPaths);
            }
        }

        List<String> fresh = client.listSecretItemPathsRecursive(folderNormalized);
        synchronized (LOCK) {
            cachedFolderKey = cacheKey;
            cachedTtlSeconds = ttlSec;
            cachedPaths = Collections.unmodifiableList(new ArrayList<>(fresh));
            cachedAtNanos = System.nanoTime();
        }
        LOG.log(Level.INFO, "Akeyless Credentials Provider: list-items refreshed for folder={0}, {1} path(s), cache TTL {2}s",
                new Object[]{folderNormalized, fresh.size(), ttlSec});
        return new ArrayList<>(fresh);
    }

    @Nonnull
    public static List<String> getOrLoad(
            @Nonnull AkeylessSyncedClient client,
            @Nonnull String folderNormalized,
            boolean cacheEnabled) throws ApiException {
        return getOrLoad(client, folderNormalized, cacheEnabled, null);
    }

    private static String cacheKey(@Nullable String ownerUserId, String folderNormalized) {
        String owner = ownerUserId != null && !ownerUserId.isBlank() ? ownerUserId : "@global";
        return owner + "|" + folderNormalized;
    }
}
