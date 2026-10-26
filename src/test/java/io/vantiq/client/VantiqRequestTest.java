package io.vantiq.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import okio.BufferedSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
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
    public void testPublishService() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        vantiq.publish(Vantiq.SystemResources.SERVICES.value(), "foo", msg, handler);
        waitForCompletion();

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/services/foo"));

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
            assertThat(ex.getMessage(), is("Only 'sources', 'services' and 'topics' support publish"));
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

    //------------------------------------------------------------------------------------
    // Tests for previously untested API surface
    //------------------------------------------------------------------------------------

    @Test
    public void testSystemResourcesExcludeRemovedResources() {
        Set<String> values = new HashSet<String>();
        for (Vantiq.SystemResources r : Vantiq.SystemResources.values()) {
            values.add(r.value());
        }
        assertThat("services defined",  values, hasItem("services"));
        assertThat("documents defined", values, hasItem("documents"));

        // These resources no longer exist on the server; it rejects them as unknown
        // or "not installed", so the SDK must not advertise them.
        assertThat("scalars removed",          values, not(hasItem("scalars")));
        assertThat("analyticsmodels removed",  values, not(hasItem("analyticsmodels")));
        assertThat("images removed",           values, not(hasItem("images")));
        assertThat("videos removed",           values, not(hasItem("videos")));
        assertThat("tensorflowmodels removed", values, not(hasItem("tensorflowmodels")));
    }

    private JsonObject batchRequest(String method, String uri) {
        JsonObject headers = new JsonObject();
        headers.addProperty("Content-type", "application/json");
        JsonObject req = new JsonObject();
        req.addProperty("method", method);
        req.add("headers", headers);
        req.addProperty("uri", uri);
        return req;
    }

    @Test
    public void testBatch() throws Exception {
        // The server answers a batch with 201 and one result per sub-request.
        // DELETE sub-requests come back as 204 with no body.
        server.enqueue(new MockResponse()
                               .setResponseCode(201)
                               .setHeader("Content-Type", "application/json")
                               .setBody("[{\"status\":200,\"headers\":{},\"body\":[{\"name\":\"TestType\"}]},"
                                        + "{\"status\":204,\"headers\":{},\"body\":null}]"));

        JsonArray requests = new JsonArray();
        requests.add(batchRequest("GET", "/types?where=%7B%22name%22%3A%22TestType%22%7D"));
        requests.add(batchRequest("DELETE", "/custom/TestType/abc"));

        vantiq.batch(requests, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/batch"));
        assertThat("Valid method", request.getMethod(), is("POST"));

        JsonArray sent = gson.fromJson(request.getBody().readUtf8(), JsonArray.class);
        assertThat("All sub-requests sent", sent.size(), is(2));
        for (JsonElement e : sent) {
            assertThat("Authorization added to each sub-request",
                       e.getAsJsonObject().getAsJsonObject("headers").get("Authorization").getAsString(),
                       is("Bearer 234592dadf23412"));
        }

        assertTrue("Successful response", handler.success);
        List<JsonObject> results = handler.getBodyAsList();
        assertThat("One result per sub-request", results.size(), is(2));
        assertThat("Select status", results.get(0).get("status").getAsInt(), is(200));
        assertThat("Select body", results.get(0).getAsJsonArray("body").get(0).getAsJsonObject().get("name").getAsString(), is("TestType"));
        assertThat("Delete status", results.get(1).get("status").getAsInt(), is(204));
    }

    @Test
    public void testTargetNamespaceHeader() throws Exception {
        assertThat("No target namespace by default", vantiq.getTargetNamespace(), nullValue());

        vantiq.setTargetNamespace("otherNS");
        assertThat("Target namespace set", vantiq.getTargetNamespace(), is("otherNS"));

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody("[]"));
        vantiq.select("MyType", null, null, null, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("X-Target-Namespace header sent", request.getHeader("X-Target-Namespace"), is("otherNS"));

        // Clearing the target namespace removes the header
        vantiq.setTargetNamespace(null);
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody("[]"));
        handler.reset();
        vantiq.select("MyType", null, null, null, handler);
        waitForCompletion();

        request = server.takeRequest();
        assertThat("X-Target-Namespace header removed", request.getHeader("X-Target-Namespace"), nullValue());
    }

    @Test
    public void testIdTokenFromAuthenticate() throws Exception {
        // The setUp authentication mock did not include an idToken
        assertThat("No idToken when the server did not return one", vantiq.getIdToken(), nullValue());

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("accessToken", "newToken")
                                                .addProperty("idToken", "theIdToken")
                                                .json()));
        vantiq.authenticate("joe", "no-one-will-guess", handler);
        waitForCompletion();
        server.takeRequest();

        assertTrue("Authenticated", handler.success);
        assertThat("Access token updated", vantiq.getAccessToken(), is("newToken"));
        assertThat("idToken captured", vantiq.getIdToken(), is("theIdToken"));
    }

    @Test
    public void testRefresh() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("accessToken", "refreshedToken")
                                                .addProperty("idToken", "refreshedIdToken")
                                                .json()));

        vantiq.refresh(vantiq.getAccessToken(), handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/authenticate/refresh"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("Token sent as body", request.getBody().readUtf8(), is("234592dadf23412"));

        assertTrue("Successful response", handler.success);
        assertThat("Body is true", handler.getBodyAsBoolean(), is(true));
        assertThat("Access token replaced", vantiq.getAccessToken(), is("refreshedToken"));
        assertThat("idToken replaced", vantiq.getIdToken(), is("refreshedIdToken"));
        assertThat("Still authenticated", vantiq.isAuthenticated(), is(true));
    }

    @Test
    public void testRevoke() throws Exception {
        // The server answers a revoke with 204 and no body
        server.enqueue(new MockResponse().setResponseCode(204));

        vantiq.revoke(vantiq.getAccessToken(), handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/authenticate/revoke"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("Token sent as body", request.getBody().readUtf8(), is("234592dadf23412"));

        assertTrue("Successful response", handler.success);
        assertThat("Body is true", handler.getBodyAsBoolean(), is(true));
        assertThat("No longer authenticated", vantiq.isAuthenticated(), is(false));
        assertThat("Access token cleared", vantiq.getAccessToken(), nullValue());
    }

    @Test
    public void testGetNamespaceUsers() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonArrayBuilder()
                                                .add(new JsonObjectBuilder().addProperty("username", "joe").addProperty("preferredUsername", "Joe").obj())
                                                .json()));

        vantiq.getNamespaceUsers("myNS", handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/namespaces/myNS/authorizedUsers"));
        assertThat("Valid method", request.getMethod(), is("GET"));

        assertTrue("Successful response", handler.success);
        JsonArray users = (JsonArray) handler.getBody();
        assertThat("One user", users.size(), is(1));
        assertThat("Username", users.get(0).getAsJsonObject().get("username").getAsString(), is("joe"));
    }

    @Test
    public void testRawGet() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody("{\"buildInfo\":{\"version\":\"1.0\"}}"));

        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("limit", "5");
        Map<String,String> extraHeaders = new HashMap<String,String>();
        extraHeaders.put("X-Extra-Header", "extra");

        vantiq.rawGet("api/v1/_status", queryParams, extraHeaders, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Raw path used as-is", url.encodedPath(), is("/api/v1/_status"));
        assertThat("Valid method", request.getMethod(), is("GET"));
        assertThat("Query parameter sent", url.queryParameter("limit"), is("5"));
        assertThat("Extra header sent", request.getHeader("X-Extra-Header"), is("extra"));
        assertThat("Authorization sent", request.getHeader("Authorization"), is("Bearer 234592dadf23412"));

        assertTrue("Successful response", handler.success);
        assertThat("Body parsed", handler.getBodyAsJsonObject().getAsJsonObject("buildInfo").get("version").getAsString(), is("1.0"));
    }

    @Test
    public void testRawPost() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder().addProperty("_id", "abc").addProperty("k", 42).json()));

        JsonObject record = new JsonObjectBuilder().addProperty("k", 42).obj();
        vantiq.post("/resources/custom/MyType", record, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("Body sent as JSON", gson.fromJson(request.getBody().readUtf8(), JsonObject.class), is(record));

        assertTrue("Successful response", handler.success);
        assertThat("Response body", handler.getBodyAsJsonObject().get("_id").getAsString(), is("abc"));
    }

    @Test
    public void testRawPut() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder().addProperty("_id", "abc").addProperty("k", 13).json()));

        JsonObject record = new JsonObjectBuilder().addProperty("k", 13).obj();
        vantiq.put("/resources/custom/MyType/abc", record, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType/abc"));
        assertThat("Valid method", request.getMethod(), is("PUT"));
        assertThat("Body sent as JSON", gson.fromJson(request.getBody().readUtf8(), JsonObject.class), is(record));

        assertTrue("Successful response", handler.success);
        assertThat("Response body", handler.getBodyAsJsonObject().get("k").getAsInt(), is(13));
    }

    @Test
    public void testRawDel() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        vantiq.del("/resources/custom/MyType/abc", handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType/abc"));
        assertThat("Valid method", request.getMethod(), is("DELETE"));

        assertTrue("Successful response", handler.success);
        assertThat("Body is true", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testUploadWithResourcePath() throws Exception {
        // First request is the session check (_status), second is the upload itself
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "application/json")
                               .setResponseCode(200));
        server.enqueue(new MockResponse()
                               .setHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(new JsonObjectBuilder()
                                                .addProperty("name", "assets/testFile.txt")
                                                .addProperty("fileType", "text/plain")
                                                .addProperty("content", "/docs/assets/testFile.txt")
                                                .json()));

        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        vantiq.upload(file, "text/plain", "assets/" + fileName, "resources/documents", handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Session check first", url.encodedPath(), is("/api/v1/_status"));

        request = server.takeRequest();
        url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Upload goes to the given resource path", url.encodedPath(), is("/api/v1/resources/documents"));
        assertThat("Valid method", request.getMethod(), is("POST"));

        String multipartBody = request.getBody().readUtf8();
        String pattern = "Content-Disposition: form-data; name=\"defaultName\"; filename=\"assets/testFile.txt\"";
        assertThat("Document path used as file name", multipartBody, containsString(pattern));

        assertTrue("Successful response", handler.success);
        assertThat("Content path", handler.getBodyAsJsonObject().get("content").getAsString(), is("/docs/assets/testFile.txt"));
    }

    //------------------------------------------------------------------------------------
    // Tests for the 1.45 API additions
    //------------------------------------------------------------------------------------

    private MockResponse jsonResponse(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private HttpUrl requestUrl(RecordedRequest request) {
        return HttpUrl.parse("http://localhost" + request.getPath());
    }

    @Test
    public void testSystemResourcesIncludeCurrentResources() {
        Set<String> values = new HashSet<String>();
        for (Vantiq.SystemResources r : Vantiq.SystemResources.values()) {
            values.add(r.value());
        }
        for (String expected : Arrays.asList("secrets", "llms", "semanticindexes", "tests", "testsuites",
                                             "discussions", "projects", "groups", "tokens")) {
            assertThat(expected + " defined", values, hasItem(expected));
        }
    }

    @Test
    public void testSelectWithExcludeProps() throws Exception {
        server.enqueue(jsonResponse(200, "[]"));

        Map<String,String> options = new HashMap<String,String>();
        options.put("page", "2");
        vantiq.select("MyType", Arrays.asList("secret", "big"), true, null, new SortSpec("a", false), 25, options, handler);
        waitForCompletion();

        HttpUrl url = requestUrl(server.takeRequest());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType"));
        assertThat("props sent", url.queryParameter("props"), is("[\"secret\",\"big\"]"));
        assertThat("excludeProps sent", url.queryParameter("excludeProps"), is("true"));
        assertThat("sort sent", url.queryParameter("sort"), is("{\"a\":1}"));
        assertThat("limit sent", url.queryParameter("limit"), is("25"));
        assertThat("count sent with limit", url.queryParameter("count"), is("true"));
        assertThat("option passed through", url.queryParameter("page"), is("2"));
        assertTrue("Successful response", handler.success);
    }

    @Test
    public void testSelectWithoutExcludePropsOmitsParameter() throws Exception {
        server.enqueue(jsonResponse(200, "[]"));
        vantiq.select("MyType", Arrays.asList("a"), null, null, 0, null, handler);
        waitForCompletion();

        HttpUrl url = requestUrl(server.takeRequest());
        assertThat("props sent", url.queryParameter("props"), is("[\"a\"]"));
        assertThat("excludeProps absent", url.queryParameter("excludeProps"), nullValue());
        assertThat("limit absent", url.queryParameter("limit"), nullValue());
    }

    @Test
    public void testSelectByPost() throws Exception {
        server.enqueue(jsonResponse(200, "[{\"_id\":\"1\",\"k\":42}]"));

        JsonObject where = new JsonObjectBuilder().addProperty("k", 42).obj();
        vantiq.selectByPost("MyType", Arrays.asList("k"), false, where, null, 10, null, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = requestUrl(request);
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType/select"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("where not in query", url.queryParameter("where"), nullValue());
        assertThat("props in query", url.queryParameter("props"), is("[\"k\"]"));
        assertThat("limit in query", url.queryParameter("limit"), is("10"));
        assertThat("where sent as body", gson.fromJson(request.getBody().readUtf8(), JsonObject.class), is(where));

        assertTrue("Successful response", handler.success);
        assertThat("Body converted to list", handler.getBodyAsList().get(0).get("k").getAsInt(), is(42));
    }

    @Test
    public void testSelectByPostWithoutWhereSendsEmptyObject() throws Exception {
        server.enqueue(jsonResponse(200, "[]"));
        vantiq.selectByPost("system.types", null, false, null, null, 0, null, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", requestUrl(request).encodedPath(), is("/api/v1/resources/types/select"));
        assertThat("Empty where body", request.getBody().readUtf8(), is("{}"));
    }

    @Test
    public void testPatchOne() throws Exception {
        server.enqueue(jsonResponse(200, "{\"_id\":\"abc\",\"k\":5}"));

        JsonArray ops = gson.fromJson("[{\"op\":\"replace\",\"path\":\"/k\",\"value\":5}]", JsonArray.class);
        vantiq.patch("MyType", "abc", ops, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", requestUrl(request).encodedPath(), is("/api/v1/resources/custom/MyType/abc"));
        assertThat("Valid method", request.getMethod(), is("PATCH"));
        assertThat("Operations sent as body", gson.fromJson(request.getBody().readUtf8(), JsonArray.class), is(ops));

        assertTrue("Successful response", handler.success);
        assertThat("Updated record returned", handler.getBodyAsJsonObject().get("k").getAsInt(), is(5));
    }

    @Test
    public void testPatchOneNoContent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        vantiq.patch("MyType", "abc", new JsonArray(), handler);
        waitForCompletion();

        server.takeRequest();
        assertTrue("Successful response", handler.success);
        assertThat("Body is true when server returns no content", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testPatchBulk() throws Exception {
        server.enqueue(jsonResponse(200, "{}"));

        JsonObject where = new JsonObjectBuilder().addProperty("k", 1).obj();
        JsonArray ops = gson.fromJson("[{\"op\":\"add\",\"path\":\"/flag\",\"value\":true}]", JsonArray.class);
        vantiq.patch("MyType", where, ops, null, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = requestUrl(request);
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/custom/MyType"));
        assertThat("Valid method", request.getMethod(), is("PATCH"));
        assertThat("where in query", url.queryParameter("where"), is("{\"k\":1}"));
        assertThat("count not requested by default", url.queryParameter("count"), nullValue());

        assertTrue("Successful response", handler.success);
        assertThat("Body is true without a count header", handler.getBodyAsBoolean(), is(true));
    }

    @Test
    public void testPatchBulkWithCount() throws Exception {
        server.enqueue(jsonResponse(200, "{}").setHeader("X-Total-Count", "7"));

        Map<String,String> options = new HashMap<String,String>();
        options.put("count", "true");
        vantiq.patch("MyType", new JsonObject(), new JsonArray(), options, handler);
        waitForCompletion();

        HttpUrl url = requestUrl(server.takeRequest());
        assertThat("count requested", url.queryParameter("count"), is("true"));
        assertTrue("Successful response", handler.success);
        assertThat("Body is the count", handler.getBodyAsInt(), is(7));
    }

    @Test
    public void testGetTypeSchema() throws Exception {
        server.enqueue(jsonResponse(200, "{\"title\":\"MyType\",\"type\":\"object\",\"properties\":{\"k\":{\"type\":\"integer\"}},\"required\":[]}"));

        vantiq.getTypeSchema("MyType", handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", requestUrl(request).encodedPath(), is("/api/v1/resources/types/MyType/_schema"));
        assertThat("Valid method", request.getMethod(), is("GET"));

        assertTrue("Successful response", handler.success);
        assertThat("Schema title", handler.getBodyAsJsonObject().get("title").getAsString(), is("MyType"));
        assertThat("Schema property", handler.getBodyAsJsonObject().getAsJsonObject("properties").getAsJsonObject("k").get("type").getAsString(), is("integer"));
    }

    @Test
    public void testExecuteWithOptions() throws Exception {
        server.enqueue(jsonResponse(200, "{\"r\":1}"));

        Map<String,String> options = new HashMap<String,String>();
        options.put("maxBufferSize", "1024");
        options.put("maxFlushInterval", "250");
        vantiq.execute("myProc", new JsonObject(), options, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = requestUrl(request);
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/myProc"));
        assertThat("maxBufferSize sent", url.queryParameter("maxBufferSize"), is("1024"));
        assertThat("maxFlushInterval sent", url.queryParameter("maxFlushInterval"), is("250"));
        assertThat("stream not forced", url.queryParameter("stream"), nullValue());
        assertTrue("Successful response", handler.success);
    }

    @Test
    public void testExecuteStreamingJson() throws Exception {
        server.enqueue(jsonResponse(200, "[{\"i\":1},{\"i\":2}]"));

        vantiq.executeStreaming("myProc", new JsonObject(), null, false, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = requestUrl(request);
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/myProc"));
        assertThat("stream requested", url.queryParameter("stream"), is("true"));
        assertThat("JSON accepted", request.getHeader("Accept"), is("application/json"));

        assertTrue("Successful response", handler.success);
        assertThat("Body is a stream", handler.getBody(), instanceOf(BufferedSource.class));
        BufferedSource source = (BufferedSource) handler.getBody();
        JsonArray results = gson.fromJson(source.readUtf8(), JsonArray.class);
        source.close();
        assertThat("Two results", results.size(), is(2));
        assertThat("Second result", results.get(1).getAsJsonObject().get("i").getAsInt(), is(2));
    }

    @Test
    public void testExecuteStreamingServerSentEvents() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "text/event-stream")
                               .setBody("data: {\"i\":1}\n\ndata: {\"i\":2}\n\n"));

        Map<String,String> options = new HashMap<String,String>();
        options.put("maxBufferSize", "2048");
        vantiq.executeStreaming("myProc", new JsonObject(), options, true, handler);
        waitForCompletion();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = requestUrl(request);
        assertThat("stream requested", url.queryParameter("stream"), is("true"));
        assertThat("options kept", url.queryParameter("maxBufferSize"), is("2048"));
        assertThat("SSE accepted", request.getHeader("Accept"), is("text/event-stream"));

        assertTrue("Successful response", handler.success);
        List<JsonElement> events = ServerSentEventReader.readAll((BufferedSource) handler.getBody());
        assertThat("Two events", events.size(), is(2));
        assertThat("First event", events.get(0).getAsJsonObject().get("i").getAsInt(), is(1));
        assertThat("Second event", events.get(1).getAsJsonObject().get("i").getAsInt(), is(2));
    }

    @Test
    public void testDefaultHeadersSentOnEveryRequest() throws Exception {
        vantiq.setDefaultHeader("X-Impersonate-User", "someone");
        assertThat("Header recorded", vantiq.getDefaultHeaders().get("X-Impersonate-User"), is("someone"));

        // select (GET)
        server.enqueue(jsonResponse(200, "[]"));
        vantiq.select("MyType", null, null, null, handler);
        waitForCompletion();
        assertThat("Sent on GET", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        // insert (POST)
        server.enqueue(jsonResponse(200, "{}"));
        handler.reset();
        vantiq.insert("MyType", new JsonObject(), handler);
        waitForCompletion();
        assertThat("Sent on POST", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        // delete (DELETE)
        server.enqueue(new MockResponse().setResponseCode(204));
        handler.reset();
        vantiq.deleteOne("MyType", "abc", handler);
        waitForCompletion();
        assertThat("Sent on DELETE", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        // refresh goes through its own request builder
        server.enqueue(jsonResponse(200, "{\"accessToken\":\"t2\"}"));
        handler.reset();
        vantiq.refresh(vantiq.getAccessToken(), handler);
        waitForCompletion();
        assertThat("Sent on refresh", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        // removing the header stops it being sent
        vantiq.removeDefaultHeader("X-Impersonate-User");
        assertThat("Header removed", vantiq.getDefaultHeaders().containsKey("X-Impersonate-User"), is(false));
        server.enqueue(jsonResponse(200, "[]"));
        handler.reset();
        vantiq.select("MyType", null, null, null, handler);
        waitForCompletion();
        assertThat("Not sent after removal", server.takeRequest().getHeader("X-Impersonate-User"), nullValue());
    }

    @Test
    public void testDefaultHeaderNullValueRemoves() throws Exception {
        vantiq.setDefaultHeader("X-Extra", "1");
        vantiq.setDefaultHeader("X-Extra", null);
        assertThat("Null value removes header", vantiq.getDefaultHeaders().containsKey("X-Extra"), is(false));
    }

    @Test
    public void testRawVerbsWithExtraHeaders() throws Exception {
        Map<String,String> extra = new HashMap<String,String>();
        extra.put("X-Impersonate-User", "someone");

        server.enqueue(jsonResponse(200, "{}"));
        vantiq.post("/resources/custom/MyType", new JsonObject(), extra, handler);
        waitForCompletion();
        assertThat("POST header", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        server.enqueue(jsonResponse(200, "{}"));
        handler.reset();
        vantiq.put("/resources/custom/MyType/abc", new JsonObject(), extra, handler);
        waitForCompletion();
        assertThat("PUT header", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));

        server.enqueue(new MockResponse().setResponseCode(204));
        handler.reset();
        vantiq.del("/resources/custom/MyType/abc", extra, handler);
        waitForCompletion();
        assertThat("DELETE header", server.takeRequest().getHeader("X-Impersonate-User"), is("someone"));
        assertThat("DELETE body is true", handler.getBodyAsBoolean(), is(true));

        // Per-call headers do not stick
        server.enqueue(jsonResponse(200, "{}"));
        handler.reset();
        vantiq.post("/resources/custom/MyType", new JsonObject(), handler);
        waitForCompletion();
        assertThat("Header not retained", server.takeRequest().getHeader("X-Impersonate-User"), nullValue());
    }
}
