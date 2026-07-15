package io.jenkins.plugins.akeyless.synced.client;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies Jenkins {@link ProxyConfiguration} (Manage Jenkins → System → HTTP Proxy Configuration)
 * to an {@link OkHttpClient.Builder} used by the Akeyless Java SDK.
 */
public final class JenkinsProxyOkHttp {

    private static final Logger LOG = Logger.getLogger(JenkinsProxyOkHttp.class.getName());

    private JenkinsProxyOkHttp() {}

    /**
     * Returns a new {@link OkHttpClient} with Jenkins proxy settings applied when configured.
     */
    @NonNull
    public static OkHttpClient newClient() {
        return configure(new OkHttpClient.Builder()).build();
    }

    /**
     * Applies Jenkins proxy host/port, no-proxy hosts, and proxy authentication to the builder.
     */
    @NonNull
    public static OkHttpClient.Builder configure(@NonNull OkHttpClient.Builder builder) {
        ProxyConfiguration proxy = currentProxy();
        if (proxy == null || proxy.getName() == null || proxy.getName().isBlank()) {
            return builder;
        }
        LOG.log(Level.FINE, "Akeyless Credentials Provider: using Jenkins HTTP proxy {0}:{1}",
                new Object[]{proxy.getName(), proxy.getPort()});
        builder.proxySelector(new JenkinsProxySelector());
        builder.proxyAuthenticator(new JenkinsProxyAuthenticator());
        return builder;
    }

    @Nullable
    private static ProxyConfiguration currentProxy() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        return jenkins == null ? null : jenkins.getProxy();
    }

    private static final class JenkinsProxySelector extends ProxySelector {

        @Override
        public List<Proxy> select(URI uri) {
            ProxyConfiguration configuration = currentProxy();
            if (configuration == null) {
                return Collections.singletonList(Proxy.NO_PROXY);
            }
            return List.of(configuration.createProxy(uri.getHost()));
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            LOG.log(Level.FINE, "Akeyless Credentials Provider: proxy connection failed for {0}", uri);
        }
    }

    private static final class JenkinsProxyAuthenticator implements Authenticator {

        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, Response response) throws IOException {
            ProxyConfiguration proxy = currentProxy();
            if (proxy == null || proxy.getUserName() == null) {
                return null;
            }
            if (response.request().header("Proxy-Authorization") != null) {
                return null;
            }
            String proxyAuthenticateHeader = response.header("Proxy-Authenticate");
            if (proxyAuthenticateHeader == null) {
                return null;
            }
            if (!proxyAuthenticateHeader.toLowerCase(Locale.ROOT).startsWith("basic")) {
                LOG.log(Level.WARNING,
                        "Akeyless Credentials Provider: unsupported proxy authentication scheme: {0}",
                        proxyAuthenticateHeader);
                return null;
            }
            String credential = Credentials.basic(proxy.getUserName(), Secret.toString(proxy.getSecretPassword()));
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        }
    }
}
