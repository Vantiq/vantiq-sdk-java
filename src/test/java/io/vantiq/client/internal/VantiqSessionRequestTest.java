package io.vantiq.client.internal;

import com.google.gson.JsonObject;
import okhttp3.mockwebserver.MockResponse;
import io.vantiq.client.VantiqResponse;
import io.vantiq.client.VantiqTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
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
        assertThat("Empty bytes body", ((byte[]) handler.getBody()).length, is(0));
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

    @Test
    public void testHandleJSONResponseSync() throws Exception {
        server.enqueue(new MockResponse()
                           .setResponseCode(200)
                           .setHeader("Content-Type", "application/json")
                           .setBody(new JsonObjectBuilder()
                                        .addProperty("a", 1.2)
                                        .addProperty("b", 2.4)
                                        .json()));

        VantiqResponse response = session.get("/resources/types", null, null);
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsDouble(), is(1.2));
    }

    @Test
    public void testHandleEmptyResponseSync() throws Exception {
        server.enqueue(new MockResponse()
                           .setResponseCode(200));

        VantiqResponse response = session.get("/resources/types", null, null);
        assertTrue("Successful response", response.isSuccess());
        assertThat("Empty bytes body", ((byte[]) response.getBody()).length, is(0));
    }

    @Test
    public void testHandleNonJSONResponseSync() throws Exception {
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "text/plain")
                           .setResponseCode(200)
                           .setBody("Hello!"));

        VantiqResponse response = session.get("/resources/types", null, null);
        assertTrue("Successful response", response.isSuccess());
        assertThat("Plain text body", (String) response.getBody(), is("Hello!"));
    }

    @Test
    public void testUpload() throws Exception {
        server.enqueue(new MockResponse()
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(200)
                            .setBody(new JsonObjectBuilder()
                                .addProperty("a", 1.2)
                                .addProperty("b", 2.4)
                                .json()));

        session.upload("/resources/documents",
                       new File(this.getClass().getResource("/testFile.txt").getFile()),
                       "text/plain",
                       "Test File",
                       null,
                       handler);
        waitForCompletion();
        assertTrue("Successful response", handler.success);
    }

    @Test
    public void testUploadSync() throws Exception {
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "application/json")
                           .setResponseCode(200)
                           .setBody(new JsonObjectBuilder()
                                        .addProperty("a", 1.2)
                                        .addProperty("b", 2.4)
                                        .json()));

        VantiqResponse response = session.upload("/resources/documents",
                                                 new File(this.getClass().getResource("/testFile.txt").getFile()),
                                                 "text/plain",
                                                 "Test File",
                                                 null,
                                                 null);
        assertTrue("Successful response", response.isSuccess());
    }
}
