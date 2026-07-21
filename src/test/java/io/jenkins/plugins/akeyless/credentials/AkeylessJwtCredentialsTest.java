package io.jenkins.plugins.akeyless.credentials;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.util.Secret;
import io.akeyless.client.model.Auth;
import org.junit.jupiter.api.Test;

class AkeylessJwtCredentialsTest {

    @Test
    void payloadContainsAuthWithJwt() {
        AkeylessJwtCredentials creds = new AkeylessJwtCredentials(null, null, null);
        creds.setAccessId("p-jwt");
        creds.setJwt(Secret.fromString("header.payload.signature"));

        CredentialsPayload payload = creds.getCredentialsPayload();
        Auth auth = payload.getAuth();
        assertThat(auth.getAccessId(), is("p-jwt"));
        assertThat(auth.getAccessType(), is("jwt"));
        assertThat(auth.getJwt(), is("header.payload.signature"));
    }
}
