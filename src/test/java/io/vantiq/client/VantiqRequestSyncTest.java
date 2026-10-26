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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

/**
 * Mocked unit tests that exercise the request and
 * response processing support for Vantiq.  These tests
 * use the synchronous form for the methods.
 */
public class VantiqRequestSyncTest extends VantiqTestBase {

    private Vantiq vantiq;

    public VantiqRequestSyncTest() {
        super(false);
    }

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
        vantiq.authenticate("joe", "no-one-will-guess");
        assertThat("Authenticated", vantiq.isAuthenticated(), is(true));
        server.takeRequest();
    }

    @After
    public void tearDownVantiq() {
        vantiq = null;
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

        VantiqResponse response = vantiq.select("MyType", null, null, null);
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("a").getAsInt(), is(1));
    }

    @Test
    public void testSelectQueryWithOptions() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(new JsonArrayBuilder()
                        .add(new JsonObjectBuilder().addProperty("a", 1).addProperty("b", "bingo").obj())
                        .add(new JsonObjectBuilder().addProperty("a", 2).addProperty("b", "jenga").obj())
                        .json()));

        Map<String,String> options = null;
        options = new HashMap<String,String>();
        options.put("AnyOption","true");

        VantiqResponse response = vantiq.select("MyType", null, null, null, 0, options);
        assertTrue("Successful response", response.isSuccess());
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

        VantiqResponse response = vantiq.select("MyType", props, where, sort);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid props", url.queryParameter("props"), is("[\"_id\",\"b\"]"));
        assertThat("Valid where", url.queryParameter("where"), is("{\"a\":1}"));
        assertThat("Valid sort",  url.queryParameter("sort"),  is("{\"a\":-1}"));

        // Check the response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((List<JsonObject>) response.getBody()).get(0).get("b").getAsString(), is("bingo"));
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

        VantiqResponse response = vantiq.selectOne("MyType", "abc123");

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType/abc123"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("bingo"));
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

        VantiqResponse response = vantiq.count("MyType", null);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Count", (Integer) response.getBody(), is(2));
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

        VantiqResponse response = vantiq.count("MyType", where);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Count parameter", url.queryParameter("count"), is("true"));
        assertThat("Props",           url.queryParameter("props"), is("[\"_id\"]"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Count", (Integer) response.getBody(), is(1));
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

        VantiqResponse response = vantiq.insert("MyType", obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
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

        VantiqResponse response = vantiq.update("MyType", key, obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/api/v1/resources/custom/MyType/12345"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
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

        VantiqResponse response = vantiq.upsert("MyType", obj);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",       url.encodedPath(),            is("/api/v1/resources/custom/MyType"));
        assertThat("Upsert parameter", url.queryParameter("upsert"), is("true"));

        JsonObject reqObj = gson.fromJson(readAll(request.getBody()), JsonObject.class);
        assertThat("Valid request body", reqObj.get("a").getAsInt(),    is(1));
        assertThat("Valid request body", reqObj.get("b").getAsString(), is("foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("a").getAsInt(), is(1));
        assertThat("Valid body", ((JsonObject) response.getBody()).get("b").getAsString(), is("foo"));
    }

    @Test
    public void testDelete() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204)
                               .setHeader("X-Total-Count", 2));


        JsonObject where = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.delete("MyType", where);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/custom/MyType"));
        assertThat("Count parameter", url.queryParameter("count"), is("true"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testDeleteOne() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(204));

        VantiqResponse response = vantiq.deleteOne("MyType", "12345");

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path",      url.encodedPath(),           is("/api/v1/resources/custom/MyType/12345"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPublishTopic() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.publish(Vantiq.SystemResources.TOPICS.value(), "/foo/bar", msg);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/topics//foo/bar"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPublishService() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.publish(Vantiq.SystemResources.SERVICES.value(), "foo", msg);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/services/foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPublishSource() throws Exception {
        server.enqueue(new MockResponse()
                               .setResponseCode(200));

        JsonObject msg = new JsonObjectBuilder().addProperty("a", 1).obj();

        VantiqResponse response = vantiq.publish(Vantiq.SystemResources.SOURCES.value(), "foo", msg);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/foo"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Result", ((Boolean) response.getBody()), is(true));
    }

    @Test
    public void testPreventPublishOnOtherTypes() throws Exception {
        try {
            vantiq.publish(Vantiq.SystemResources.TYPES.value(), "MyType", null);
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

        VantiqResponse response = vantiq.execute("adder", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/adder"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("total").getAsInt(), is(3));
    }

    @Test
    public void testMissingProcedure() throws Exception {
        server.enqueue(new MockResponse()
                                .setResponseCode(404));

        JsonObject params = new JsonObjectBuilder()
                .addProperty("arg1", 1)
                .addProperty("arg2", 2)
                .obj();

        VantiqResponse response = vantiq.execute("whoops", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/procedures/whoops"));

        // Check response
        assertTrue("Error response",    !response.isSuccess());
        assertThat("Missing procedure", response.getStatusCode(), is(404));
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

        VantiqResponse response = vantiq.query("adder", params);

        // We first check the request
        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/sources/adder/query"));

        // Check response
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body", ((JsonObject) response.getBody()).get("total").getAsInt(), is(3));
    }

    @Test
    public void testSubscribeServiceEvent() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody(new JsonObjectBuilder()
                        .json())
        );

        vantiq.subscribe(Vantiq.SystemResources.SERVICES.value(),
                                                    "myService/myEvent",
                                                        null,
                                                        null);
    }

    @Test
    public void testUploadInvalidSession() throws Exception {
        server.enqueue(new MockResponse()
                           .setHeader("Content-Type", "application/json")
                           .setResponseCode(401));

        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        VantiqResponse response = vantiq.upload(file, "text/plain", fileName);

        // Assert that we had an invalid response
        assertTrue("Error response", !response.isSuccess());
        assertThat("Invalid Authentication Status", response.getStatusCode(), is(401));
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
        VantiqResponse response = vantiq.upload(file, "text/plain", fileName);

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
        assertTrue("Successful response", response.isSuccess());
        assertThat("Valid body - name", ((JsonObject) response.getBody()).get("name").getAsString(), is("testFile.txt"));
        assertThat("Valid body - fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is("text/plain"));
    }

    //------------------------------------------------------------------------------------
    // Tests for previously untested API surface (synchronous forms)
    //------------------------------------------------------------------------------------

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
        server.enqueue(new MockResponse()
                               .setResponseCode(201)
                               .setHeader("Content-Type", "application/json")
                               .setBody("[{\"status\":200,\"headers\":{},\"body\":[{\"name\":\"TestType\"}]},"
                                        + "{\"status\":204,\"headers\":{},\"body\":null}]"));

        JsonArray requests = new JsonArray();
        requests.add(batchRequest("GET", "/types?where=%7B%22name%22%3A%22TestType%22%7D"));
        requests.add(batchRequest("DELETE", "/custom/TestType/abc"));

        VantiqResponse response = vantiq.batch(requests);

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/batch"));
        assertThat("Valid method", request.getMethod(), is("POST"));

        JsonArray sent = gson.fromJson(request.getBody().readUtf8(), JsonArray.class);
        for (JsonElement e : sent) {
            assertThat("Authorization added to each sub-request",
                       e.getAsJsonObject().getAsJsonObject("headers").get("Authorization").getAsString(),
                       is("Bearer 234592dadf23412"));
        }

        assertTrue("Successful response", response.isSuccess());
        @SuppressWarnings("unchecked")
        List<JsonObject> results = (List<JsonObject>) response.getBody();
        assertThat("One result per sub-request", results.size(), is(2));
        assertThat("Select status", results.get(0).get("status").getAsInt(), is(200));
        assertThat("Delete status", results.get(1).get("status").getAsInt(), is(204));
    }

    @Test
    public void testTargetNamespaceHeader() throws Exception {
        vantiq.setTargetNamespace("otherNS");
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody("[]"));
        vantiq.select("MyType", null, null, null);

        RecordedRequest request = server.takeRequest();
        assertThat("X-Target-Namespace header sent", request.getHeader("X-Target-Namespace"), is("otherNS"));
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

        VantiqResponse response = vantiq.refresh(vantiq.getAccessToken());

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/authenticate/refresh"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("Token sent as body", request.getBody().readUtf8(), is("234592dadf23412"));

        assertTrue("Successful response", response.isSuccess());
        assertThat("Body is true", (Boolean) response.getBody(), is(true));
        assertThat("Access token replaced", vantiq.getAccessToken(), is("refreshedToken"));
        assertThat("idToken replaced", vantiq.getIdToken(), is("refreshedIdToken"));
        assertThat("Still authenticated", vantiq.isAuthenticated(), is(true));
    }

    @Test
    public void testRevoke() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        VantiqResponse response = vantiq.revoke(vantiq.getAccessToken());

        RecordedRequest request = server.takeRequest();
        assertThat("Valid path", request.getPath(), is("/authenticate/revoke"));
        assertThat("Valid method", request.getMethod(), is("POST"));
        assertThat("Token sent as body", request.getBody().readUtf8(), is("234592dadf23412"));

        assertTrue("Successful response", response.isSuccess());
        assertThat("Body is true", (Boolean) response.getBody(), is(true));
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

        VantiqResponse response = vantiq.getNamespaceUsers("myNS");

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Valid path", url.encodedPath(), is("/api/v1/resources/namespaces/myNS/authorizedUsers"));
        assertThat("Valid method", request.getMethod(), is("GET"));

        assertTrue("Successful response", response.isSuccess());
        JsonArray users = (JsonArray) response.getBody();
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

        VantiqResponse response = vantiq.rawGet("api/v1/_status", queryParams, extraHeaders);

        RecordedRequest request = server.takeRequest();
        HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
        assertThat("Raw path used as-is", url.encodedPath(), is("/api/v1/_status"));
        assertThat("Query parameter sent", url.queryParameter("limit"), is("5"));
        assertThat("Extra header sent", request.getHeader("X-Extra-Header"), is("extra"));

        assertTrue("Successful response", response.isSuccess());
        assertThat("Body parsed", ((JsonObject) response.getBody()).getAsJsonObject("buildInfo").get("version").getAsString(), is("1.0"));
    }

    @Test
    public void testRawPostPutDel() throws Exception {
        JsonObject record = new JsonObjectBuilder().addProperty("k", 42).obj();

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder().addProperty("_id", "abc").addProperty("k", 42).json()));
        VantiqResponse response = vantiq.post("/resources/custom/MyType", record);
        RecordedRequest request = server.takeRequest();
        assertThat("POST path", HttpUrl.parse("http://localhost" + request.getPath()).encodedPath(), is("/api/v1/resources/custom/MyType"));
        assertThat("POST method", request.getMethod(), is("POST"));
        assertThat("POST body", gson.fromJson(request.getBody().readUtf8(), JsonObject.class), is(record));
        assertTrue("POST succeeded", response.isSuccess());
        assertThat("POST response", ((JsonObject) response.getBody()).get("_id").getAsString(), is("abc"));

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(new JsonObjectBuilder().addProperty("_id", "abc").addProperty("k", 13).json()));
        record.addProperty("k", 13);
        response = vantiq.put("/resources/custom/MyType/abc", record);
        request = server.takeRequest();
        assertThat("PUT path", HttpUrl.parse("http://localhost" + request.getPath()).encodedPath(), is("/api/v1/resources/custom/MyType/abc"));
        assertThat("PUT method", request.getMethod(), is("PUT"));
        assertTrue("PUT succeeded", response.isSuccess());
        assertThat("PUT response", ((JsonObject) response.getBody()).get("k").getAsInt(), is(13));

        server.enqueue(new MockResponse().setResponseCode(204));
        response = vantiq.del("/resources/custom/MyType/abc");
        request = server.takeRequest();
        assertThat("DELETE path", HttpUrl.parse("http://localhost" + request.getPath()).encodedPath(), is("/api/v1/resources/custom/MyType/abc"));
        assertThat("DELETE method", request.getMethod(), is("DELETE"));
        assertTrue("DELETE succeeded", response.isSuccess());
        assertThat("DELETE body is true", (Boolean) response.getBody(), is(true));
    }
}
