package io.jenkins.plugins.akeyless.synced.auth;

import hudson.Extension;
import hudson.model.Descriptor;
import io.akeyless.client.model.Auth;
import io.jenkins.plugins.akeyless.cloudid.CloudIdProvider;
import io.jenkins.plugins.akeyless.cloudid.CloudProviderFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * AWS IAM authentication. Obtains cloud identity from EC2/ECS/env (SigV4-signed STS GetCallerIdentity).
 */
public class AwsIamSyncedAuthMethod extends SyncedAuthMethod {

    private static final Logger LOG = Logger.getLogger(AwsIamSyncedAuthMethod.class.getName());
    private static final String ACCESS_TYPE = "aws_iam";

    @DataBoundConstructor
    public AwsIamSyncedAuthMethod() {}

    @Override
    public Auth buildAuth(@Nullable String accessId) throws Exception {
        LOG.log(Level.INFO, "Akeyless AWS IAM auth: building auth for access_id={0}", accessId);
        String cloudId;
        try {
            CloudIdProvider idProvider = CloudProviderFactory.getCloudIdProvider(ACCESS_TYPE);
            cloudId = idProvider.getCloudId();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to generate AWS cloud ID", e);
            throw new Exception(
                    "AWS IAM auth: could not obtain cloud identity. "
                            + "Ensure Jenkins is running on AWS with an IAM role attached. " + e.getMessage(),
                    e);
        }
        Auth auth = new Auth();
        auth.setAccessId(accessId);
        auth.setAccessType(ACCESS_TYPE);
        auth.setCloudId(cloudId);
        LOG.log(
                Level.INFO,
                "Akeyless AWS IAM auth: sending auth request (access_id={0}, cloud_id_length={1})",
                new Object[] {accessId, cloudId.length()});
        return auth;
    }

    @Override
    public boolean isConfigured(@Nullable String accessId) {
        return accessId != null && !accessId.isBlank();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SyncedAuthMethod> {
        @Override
        public String getDisplayName() {
            return "AWS IAM";
        }
    }
}
