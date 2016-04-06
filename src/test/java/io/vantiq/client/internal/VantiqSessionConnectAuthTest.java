package io.vantiq.client.internal;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqTestBase;
import io.vantiq.client.internal.VantiqSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * Mocked unit tests that exercise the connection and
 * authentication support for VantiqSession.
 */
public class VantiqSessionConnectAuthTest extends VantiqTestBase {

    private VantiqSession session;

    @Before
    public void setUpSession() {
        session = new VantiqSession(server.url("/").toString());
    }

    @After
    public void tearDownSession() {
        session = null;
    }

    @Test
    public void testAuthenticate() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                    .addProperty("accessToken", "abc123")
                                    .json()));

        session.authenticate(USERNAME, PASSWORD, handler);
        waitForCompletion();
        assertTrue("Successful response",   handler.success);
        assertTrue("Session authenticated", session.isAuthenticated());
        assertThat("Valid access token",    session.getAccessToken(), is("abc123"));
    }

    @Test
    public void testHandleAuthenticationFailure() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(401)
                               .setHeader("Content-Type", "application/json")
                               .setBody(toJson(Collections.singletonList(new VantiqError(
                                   "io.vantiq.server.error",
                                   "Unauthorized",
                                   Collections.emptyList()
                               )))));

        session.authenticate("bad", "user", handler);
        waitForCompletion();

        assertTrue("Should not be success",     !handler.success);
        assertTrue("Should be error",           handler.error);
        assertTrue("Session not authenticated", !session.isAuthenticated());
        assertThat("Should be no access token", session.getAccessToken(), is(nullValue()));
        assertThat("HTTP Status", handler.getStatusCode(),              is(401));
        assertThat("Error code",  handler.getErrors().get(0).getCode(), is("io.vantiq.server.error"));
    }

    @Test
    public void testPassTokenOnAuthenticatedSession() throws Exception {
        // First authenticate, then issue request
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                    .addProperty("accessToken", "abc123")
                                    .json()));
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "application/json")
                               .setResponseCode(200));

        // Issue authentication request
        session.authenticate(USERNAME, PASSWORD, handler);
        waitForCompletion();
        server.takeRequest();

        // Issue subsequent request
        session.get("/resources/types", new HashMap<String, String>(), handler.reset());
        waitForCompletion();
        RecordedRequest request = server.takeRequest();

        assertThat("Correct path", request.getPath(), is("/api/v1/resources/types"));
        assertThat("Ensure token", request.getHeader("Authorization"), is("Bearer " + session.getAccessToken()));
        assertTrue(session.isAuthenticated());
    }

    @Test
    public void testUnauthorizedGet() throws Exception {
        try {
            session.get("/resources/types", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedPut() throws Exception {
        try {
            session.put("/resources/types/abc123", null, null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedPost() throws Exception {
        try {
            session.post("/resources/types", null, null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedDelete() throws Exception {
        try {
            session.delete("/resources/types/abc123", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), is("Not authenticated"));
        }
    }

}
