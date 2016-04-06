package io.vantiq.client.internal;

import com.squareup.okhttp.mockwebserver.MockResponse;
import io.vantiq.client.VantiqTestBase;
import io.vantiq.client.internal.VantiqSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Mocked unit tests that exercise the request and
 * response processing support for VantiqSession.
 */
public class VantiqSessionRequestTest extends VantiqTestBase {

    private VantiqSession session;

    @Before
    public void setUpAuth() throws Exception {
        session = new VantiqSession(server.url("/").toString());

        // Mock out the authentication
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                    .addProperty("accessToken", "234592dadf23412")
                                    .json()));

        // Run the authentication request
        session.authenticate("joe", "no-one-will-guess", handler);
        server.takeRequest();
        waitForCompletion();

        // Reset handler to setup for next call
        handler.reset();
    }

    @After
    public void tearDownSession() {
        session = null;
    }

    @Test
    public void testHandleJSONResponse() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                    .addProperty("a", 1.2)
                                    .addProperty("b", 2.4)
                                    .json()));

        session.get("/resources/types", null, handler);
        waitForCompletion();
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("a").getAsDouble(), is(1.2));
    }

    @Test
    public void testHandleEmptyResponse() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        session.get("/resources/types", null, handler);
        waitForCompletion();
        assertTrue("Successful response", handler.success);
        assertThat("Empty body", handler.getBodyAsString(), is(""));
    }

    @Test
    public void testHandleNonJSONResponse() throws Exception {
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "text/plain")
                               .setResponseCode(200)
                               .setBody("Hello!"));

        session.get("/resources/types", null, handler);
        waitForCompletion();
        assertTrue("Successful response", handler.success);
        assertThat("Plain text body", handler.getBodyAsString(), is("Hello!"));
    }

}
