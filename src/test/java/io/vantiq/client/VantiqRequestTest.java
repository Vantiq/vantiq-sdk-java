package io.vantiq.client;

import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

/**
 * Mocked unit tests that exercise the request and
 * response processing support for Vantiq.
 */
public class VantiqRequestTest extends VantiqTestBase {

    private Vantiq vantiq;

    @Before
    public void setUpVantiq() throws Exception {
        vantiq = new Vantiq(server.url("/").toString());

        // Mock out the authentication
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("accessToken", "234592dadf23412")
                                                .json()));

        // Run the authentication request
        vantiq.authenticate("joe", "no-one-will-guess", handler);
        server.takeRequest();
        waitForCompletion();

        // Reset handler to setup for next call
        handler.reset();
    }

    @After
    public void tearDownVantiq() {
        vantiq = null;
    }

    @Test
    public void testSystemResources() {
        // Simple test to ensure that system resources are defined
        assertThat("SystemResources", Vantiq.SystemResources.TYPES.value(), is("types"));
    }

    @Test
    public void testSelectQuery() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .add(new JsonObjectBuilder().addProperty("a", 2).addProperty("b", "jenga").obj())
                                                .json()));

        vantiq.select("MyType", null, null, null, handler);
        waitForCompletion();
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsList().get(0).get("a").getAsInt(), is(1));
    }

    @Test
    public void testSelectQueryWithConstraints() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .json()));

        List<String>  props = Arrays.asList("_id", "b");
        JsonObject    where = new JsonObjectBuilder().addProperty("a", 1).obj();
        SortSpec       sort = new SortSpec("a", true);

        vantiq.select("MyType", props, where, sort, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid props", url.queryParameter("props"), is("[\"_id\",\"b\"]"));
        assertThat("Valid where", url.queryParameter("where"), is("{\"a\":1}"));
        assertThat("Valid sort",  url.queryParameter("sort"),  is("{\"a\":-1}"));

        // Check the response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsList().get(0).get("a").getAsInt(), is(1));
        assertThat("Valid body", handler.getBodyAsList().get(0).get("b").getAsString(), is("bingo"));
    }

    @Test
    public void testSelectOne() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "bingo")
                                                .json()));

        vantiq.selectOne("MyType", "abc123", handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType/abc123"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("a").getAsInt(), is(1));
        assertThat("Valid body", handler.getBodyAsJsonObject().get("b").getAsString(), is("bingo"));
    }

    @Test
    public void testCountQuery() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setHeader("X-Total-Count", 2)
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .add(new JsonObjectBuilder().addProperty("a", 2).addProperty("b", "jenga").obj())
                                                .json()));

        vantiq.count("MyType", null, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Count", handler.getBodyAsInt(), is(2));
    }

    @Test
    public void testCountQueryWithConstraints() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setHeader("X-Total-Count", 1)
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                                                .json()));

        JsonObject where = new JsonObjectBuilder().addProperty("a", 1).obj();

        vantiq.count("MyType", where, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Count", handler.getBodyAsInt(), is(1));
    }

    @Test
    public void testInsert() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                            .addProperty("a", 1)
                            .addProperty("b", "foo")
                            .obj();

        vantiq.insert("MyType", obj, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("a").getAsInt(), is(1));
        assertThat("Valid body", handler.getBodyAsJsonObject().get("b").getAsString(), is("foo"));
    }

    @Test
    public void testUpdate() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                .addProperty("a", 1)
                .addProperty("b", "foo")
                .obj();
        String key = "12345";

        vantiq.update("MyType", key, obj, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType/12345"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("a").getAsInt(), is(1));
        assertThat("Valid body", handler.getBodyAsJsonObject().get("b").getAsString(), is("foo"));
    }

    @Test
    public void testUpsert() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("a", 1)
                                                .addProperty("b", "foo")
                                                .json()));

        JsonObject obj = new JsonObjectBuilder()
                .addProperty("a", 1)
                .addProperty("b", "foo")
                .obj();

        vantiq.upsert("MyType", obj, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",       url.encodedPath(),            is("/api/v1/resources/custom/MyType"));
        assertThat("Upsert parameter", url.queryParameter("upsert"), is("true"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("a").getAsInt(), is(1));
        assertThat("Valid body", handler.getBodyAsJsonObject().get("b").getAsString(), is("foo"));
    }

    @Test
    public void testDelete() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204)
                               .setHeader("X-Total-Count", 2));


        JsonObject where = new JsonObjectBuilder().addProperty("a", 1).obj();

        vantiq.delete("MyType", where, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/custom/MyType"));
        assertThat("Count parameter", url.queryParameter("count"), is("true"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Result", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testDeleteOne() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204));

        vantiq.deleteOne("MyType", "12345", handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/custom/MyType/12345"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Result", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testPublishTopic() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        vantiq.publish(Vantiq.SystemResources.TOPICS.value(), "/foo/bar", msg, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/topics//foo/bar"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Result", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testPublishSource() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        vantiq.publish(Vantiq.SystemResources.SOURCES.value(), "foo", msg, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/foo"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Result", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testPreventPublishOnOtherTypes() throws Exception {
        try {
            vantiq.publish(Vantiq.SystemResources.TYPES.value(), "MyType", null, handler);
            fail("Should only allow publish on sources and topics");
        } catch(IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Only 'sources' and 'topics' support publish"));
        }
    }

    @Test
    public void testExecuteProcedure() throws Exception {
        server.enqueue(new MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setResponseCode(200)
                                .setBody(new JsonObjectBuilder()
                                    .addProperty("total", 3)
                                    .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        vantiq.execute("adder", params, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/adder"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("total").getAsInt(), is(3));
    }

    @Test
    public void testMissingProcedure() throws Exception {
        server.enqueue(new MockResponse()
                                .setResponseCode(404));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        vantiq.execute("whoops", params, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/whoops"));

        // Check response
        assertTrue("Error response",    handler.error);
        assertThat("Missing procedure", handler.getStatusCode(), is(404));
    }


    @Test
    public void testEvaluateModel() throws Exception {
        server.enqueue(new MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setResponseCode(200)
                                .setBody(new JsonObjectBuilder()
                                    .addProperty("total", 3)
                                    .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        vantiq.evaluate("testModel", params, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/analyticsmodels/testModel"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("total").getAsInt(), is(3));
    }

    @Test
    public void testQuerySource() throws Exception {
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("total", 3)
                                                .json()));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        vantiq.query("adder", params, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/adder/query"));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body", handler.getBodyAsJsonObject().get("total").getAsInt(), is(3));
    }

    @Test
    public void testUploadInvalidSession() throws Exception {
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "application/json")
                           .setResponseCode(401));

        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        vantiq.upload(file, "text/plain", fileName, handler);
        waitForCompletion();

        // Assert that we had an invalid response
        assertTrue("Error response", !handler.success);
        assertThat("Invalid Authentication Status", handler.getStatusCode(), is(401));
    }

    @Test
    public void testUpload() throws Exception {
        // Note that this needs to respond to 2 requests.  First, there is the
        // verify session (_status) request, then there is the main request.
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "application/json")
                           .setResponseCode(200));
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "application/json")
                           .setResponseCode(200)
                           .setBody(new JsonObjectBuilder()
                                        .addProperty("name", "testFile.txt")
                                        .addProperty("fileType", "text/plain")
                                        .addProperty("content", "/docs/assets/someplace/testFile.txt")
                                        .json()));

        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        vantiq.upload(file, "text/plain", fileName, handler);
        waitForCompletion();

        // We check the first request to be the session validation
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/_status"));
        assertThat("Valid method", request.getMethod(), is("GET"));

        // We check the second request to be the upload
        request = server.takeRequest();
        url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/documents"));
        assertThat("Valid method", request.getMethod(), is("POST"));

        // Check that request has the proper headers
        String multipartBody = request.getBody().readUtf8();
        String pattern = "Content-Disposition: form-data; name=\"defaultName\"; filename=\"testFile.txt\"";
        assertThat("Valid multi-part header", multipartBody, containsString(pattern));

        // Check response
        assertTrue("Successful response", handler.success);
        assertThat("Valid body - name", ((JsonObject) handler.getBody()).get("name").getAsString(), is("testFile.txt"));
        assertThat("Valid body - fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is("text/plain"));
    }

}
