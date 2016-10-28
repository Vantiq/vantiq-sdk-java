package io.vantiq.client;

import okhttp3.mockwebserver.MockResponse;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Mocked unit tests that exercise the request and
 * response processing support for Vantiq.
 */
public class VantiqConnectAuthTest extends VantiqTestBase {

    private Vantiq vantiq;

    @Before
    public void setUpVantiq() {
        vantiq = new Vantiq(server.url("/").toString());
    }

    @After
    public void tearDownVantiq() {
        vantiq = null;
    }

    @Test
    public void testAuthenticate() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("accessToken", "abc123")
                                                .json()));

        vantiq.authenticate(USERNAME, PASSWORD, handler);
        waitForCompletion();
        assertTrue("Successful response",   handler.success);
        assertTrue("Session authenticated", vantiq.isAuthenticated());
        assertThat("Result", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testUnauthorizedSelect() throws Exception {
        try {
            vantiq.select("TestType", null, null, null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedSelectOne() throws Exception {
        try {
            vantiq.selectOne("TestType", "abc123", handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedCount() throws Exception {
        try {
            vantiq.count("TestType", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedInsert() throws Exception {
        try {
            vantiq.insert("TestType", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedUpdate() throws Exception {
        try {
            vantiq.update("TestType", null, null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedUpsert() throws Exception {
        try {
            vantiq.upsert("TestType", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedDelete() throws Exception {
        try {
            vantiq.delete("TestType", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedDeleteOne() throws Exception {
        try {
            vantiq.deleteOne("TestType", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedPublish() throws Exception {
        try {
            vantiq.publish("topics", "/test/topic", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedExecute() throws Exception {
        try {
            vantiq.execute("testProcedure", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedQuery() throws Exception {
        try {
            vantiq.query("testSource", null, handler);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }

    @Test
    public void testUnauthorizedSubscribe() throws Exception {
        try {
            vantiq.subscribe("topics", "/test/topic", null, null);
            fail("Should not allow unauthenticated requests");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(), CoreMatchers.is("Not authenticated"));
        }
    }
}
