package io.vantiq.client.intg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import io.vantiq.client.*;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okio.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Mocked unit tests for the Vantiq
 */
@Category(IntegrationTests.class)
public class VantiqIntegrationTest {

    private Vantiq vantiq;
    private final UnitTestResponseHandler handler = new UnitTestResponseHandler();

    private static String server   = null;
    private static String username = null;
    private static String password = null;
    private static String token = null;
    private static boolean usingProxy = false;
    
    @BeforeClass
    public static void setUpIntgTest() throws Exception {
        // Pull values from java properties, so the credentials are not checked in
        server   = System.getProperty("server");
        username = System.getProperty("username");
        password = System.getProperty("password");
        token = System.getProperty("token");
        
        Boolean missingUserPass = (username == null || password == null);
        Boolean missingToken = token == null;
        if(server == null || (missingUserPass && missingToken)) {
            throw new IllegalStateException("Must set 'server', 'username', and 'password', or 'token' Java System Properties");
        }
        String scheme = new URI(server).getScheme();
        usingProxy = System.getProperty(scheme + ".proxyHost") != null;
    }

    @Before
    public void setUp() throws Exception {
        doSetup(null);
    }
    
    void doSetup(Authenticator auth) throws Exception {
        if (auth == null) {
            vantiq = new Vantiq(server);
        } else {
            vantiq = new Vantiq(server, auth);
        }
        if (username != null && !username.equals("") && password != null && !password.equals("")) {
            vantiq.authenticate(username, password);
        } else if (token != null && !token.equals("")) {
            vantiq.setAccessToken(token);
        } else {
            throw new IllegalStateException("Must set 'server', 'username', and 'password', or 'token' Java System Properties");
        }
        waitForCompletion();
        assertThat("Authenticated: " + handler, vantiq.isAuthenticated(), is(true));
        handler.reset();
    }

    @After
    public void tearDown() throws Exception {
        vantiq = null;
    }

    //------------------------------------------------------------------------------------
    // Helper methods
    //------------------------------------------------------------------------------------

    private void waitForCompletion() throws InterruptedException {
        synchronized (handler) {
            handler.wait(2000);
        }
    }

    private String getISOString(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(d);
    }

    //------------------------------------------------------------------------------------
    // Integration Tests
    //------------------------------------------------------------------------------------

    @Test
    public void testSelect() throws Exception {
        performSelectTest();
    }
    
    @Test
    public void testSelectUserAuthenticator() throws Exception {
        // Create a new Vantiq instance using user-supplied authenticator
        vantiq = null;
        String scheme = new URI(server).getScheme();
        String proxyUser = System.getProperty(scheme + ".proxyUser");
        String proxyPw = System.getProperty(scheme + ".proxyPassword");
        // We'll run with or without proxy and/or authentication as  we want to ensure that this is harmless in all
        // cases
        try {
             // Here, we clear the properties so that we know our authenticator is operating
             // We do this  inside the try/finally block so that we know they get reset at the end of the try block.
             System.clearProperty(scheme + ".proxyUser");
             System.clearProperty(scheme + ".proxyPassword");
    
             Authenticator okAuth = new Authenticator() {
                @Override
                public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                    String credential = Credentials.basic(proxyUser, proxyPw);
                    return response.request().newBuilder()
                                   .header("Proxy-Authorization", credential)
                                   .build();
                }
            };
            doSetup(okAuth);
            performSelectTest();
        } finally {
            // Reset system properties
            if (proxyUser != null) {
                System.setProperty(scheme + ".proxyUser", proxyUser);
            }
            if (proxyPw != null) {
                System.setProperty(scheme + ".proxyPassword", proxyPw);
            }
        }
    }
    
    void performSelectTest() throws Exception {
        vantiq.select("system.types", null, null, null, handler);
        waitForCompletion();

        JsonObject match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if("TestType".equals(obj.get("name").getAsString())) {
                match = obj;
            }
        }
        assertThat("TestType", match, is(notNullValue()));
    }

    @Test
    public void testSelectWithConstraints() throws Exception {
        JsonObject where = new JsonObject();
        where.addProperty("name", "TestType");

        vantiq.select("system.types", Arrays.asList("_id", "name"), where, null, handler);
        waitForCompletion();

        assertThat("Single TestType result", handler.getBodyAsList().size(), is(1));
        assertThat("Single TestType result", handler.getBodyAsList().get(0).get("name").getAsString(), is("TestType"));
    }

    @Test
    public void testSelectWithSorting() throws Exception {
        vantiq.select("system.types", Arrays.asList("_id", "name"), null, new SortSpec("name", true), handler);
        waitForCompletion();

        List<JsonObject> result = handler.getBodyAsList();
        String firstName = result.get(0).get("name").getAsString();
        String lastName  = result.get(result.size() - 1).get("name").getAsString();

        assertThat("Ensure results are sorted", firstName.compareTo(lastName), greaterThan(0));
    }

    @Test
    public void testSelectOne() throws Exception {
        vantiq.selectOne("system.types", "TestType", handler);
        waitForCompletion();

        assertThat("TestType result", handler.getBodyAsJsonObject().get("name").getAsString(), is("TestType"));
    }

    @Test
    public void testCount() throws Exception {
        vantiq.select("system.types", Collections.singletonList("_id"), null, null, handler);
        waitForCompletion();

        int countFromSelect = handler.getBodyAsList().size();
        vantiq.count("system.types", null, handler.reset());
        waitForCompletion();

        assertThat("Count match", handler.getBodyAsInt(), is(countFromSelect));
    }

    @Test
    public void testCountWithConstraints() throws Exception {
        JsonObject where = new JsonObject();
        where.addProperty("name", "TestType");

        vantiq.count("system.types", where, handler);
        waitForCompletion();

        assertThat("Count match", handler.getBodyAsInt(), is(1));
    }

    @Test
    public void testInsertAndUpdateRecord() throws Exception {
        Date now = new Date();

        String id = "IU-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record
        vantiq.insert("TestType", record, handler);
        waitForCompletion();

        // Select it back to ensure it was added successfully
        JsonObject where = new JsonObject();
        where.addProperty("id", id);
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Insert succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject inserted = handler.getBodyAsList().get(0);
        assertThat("Insert has correct value", inserted.get("k").getAsInt(), is(42));

        // Update record
        JsonObject toUpdate = new JsonObject();
        toUpdate.addProperty("k", 13);
        vantiq.update("TestType", inserted.get("_id").getAsString(), toUpdate, handler.reset());
        waitForCompletion();

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Update succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject updated = handler.getBodyAsList().get(0);
        assertThat("Update has correct value", updated.get("k").getAsInt(), is(13));
        assertThat("Update maintains other properties", updated.get("o").getAsJsonObject().get("a").getAsInt(), is(1));
    }

    @Test
    public void testUpsertRecord() throws Exception {
        Date now = new Date();

        String id = "UP-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.upsert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Upsert (insert) succeeded", handler.success);

        // Select it back to ensure it was added successfully
        JsonObject where = new JsonObject();
        where.addProperty("id", id);
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Insert succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject inserted = handler.getBodyAsList().get(0);
        assertThat("Insert has correct value", inserted.get("k").getAsInt(), is(42));

        // Modify value
        inserted.addProperty("k", 13);

        // Update record using upsert
        vantiq.upsert("TestType", inserted, handler.reset());
        waitForCompletion();
        assertTrue("Upsert (update) succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Update succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject updated = handler.getBodyAsList().get(0);
        assertThat("Update has correct value", updated.get("k").getAsInt(), is(13));
        assertThat("Update maintains other properties", updated.get("o").getAsJsonObject().get("a").getAsInt(), is(1));
    }

    @Test
    public void testDelete() throws Exception {
        Date now = new Date();

        String id = "DL-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.insert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Insert succeeded", handler.success);

        // Select all records and ensure that it was in the list
        Map<String,String> qual = new HashMap<String,String>();
        qual.put("id", id);
        vantiq.select("TestType", Arrays.asList("_id", "id"), qual, null, handler.reset());
        waitForCompletion();
        assertThat("Select found inserted", handler.getBodyAsList().size(), is(1));

        // Delete record
        JsonObject where = new JsonObject();
        where.addProperty("id", id);

        vantiq.delete("TestType", where, handler.reset());
        waitForCompletion();
        assertTrue("Delete succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", Arrays.asList("_id", "id"), qual, null, handler.reset());
        waitForCompletion();
        assertThat("Delete removed record", handler.getBodyAsList().size(), is(0));
    }

    @Test
    public void testDeleteOne() throws Exception {
        Date now = new Date();

        String id = "DLONE-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.insert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Insert succeeded", handler.success);

        // Select all records and ensure that it was in the list
        Map<String,String> qual = new HashMap<String,String>();
        qual.put("id", id);
        vantiq.select("TestType", Arrays.asList("_id", "id"), qual, null, handler.reset());
        waitForCompletion();
        assertThat("Select found inserted", handler.getBodyAsList().size(), is(1));
        JsonObject match = handler.getBodyAsList().get(0);

        // Delete record
        vantiq.deleteOne("TestType", match.get("_id").getAsString(), handler.reset());
        waitForCompletion();
        assertTrue("Delete succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", Arrays.asList("_id", "id"), qual, null, handler.reset());
        waitForCompletion();
        assertThat("Delete removed record", handler.getBodyAsList().size(), is(0));
    }

    @Test
    public void testPublishServiceEvent() throws Exception {
        Date now = new Date();

        String id = "PB-" + now.getTime();
        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("ts", getISOString(now));
        message.addProperty("x", 3.14159);
        message.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        message.add("o", embedded);

        // Publish to service event.
        // Note: The service event is defined to have an implementing path which is the same as our
        // publish topic (see resources/intgTest/services/testService.json).  Thus, if this publish works,
        // an event will actually be published to the (same) test topic, and the rule defined for that
        // (onSDKTestPublish.vail) will fire.  Thus, this test has pretty much the same content as testPublishTopic()
        // below -- just the mechanism by which the "publish" action happens is different.
        vantiq.publish(Vantiq.SystemResources.SERVICES.value(), "testService/inboundTestEvent", message, handler);
        waitForCompletion();
        assertTrue("Publish to Svc Event succeeded", handler.success);

        // Rule should insert the record into TestType
        // so select it to find the record.  However, this may take time so, adding slight delay.
        Thread.sleep(1500);

        // Select all records and ensure that it was in the list
        JsonObject where = new JsonObject();
        where.addProperty("id", id);

        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();

        assertThat("Found result", handler.getBodyAsList().size(), is(1));
        assertThat("Correct value", handler.getBodyAsList().get(0).get("k").getAsInt(), is(42));
    }

    @Test
    public void testPublishTopic() throws Exception {
        Date now = new Date();

        String id = "PBT-" + now.getTime();
        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("ts", getISOString(now));
        message.addProperty("x", 3.14159);
        message.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        message.add("o", embedded);

        // Publish to topic
        vantiq.publish("topics", "/test/topic", message, handler);
        waitForCompletion();
        assertTrue("Publish succeeded", handler.success);

        // Rule should insert the record into TestType
        // so select it to find the record.  However, this may take time so, adding slight delay.
        Thread.sleep(5000);

        // Select all records and ensure that it was in the list
        JsonObject where = new JsonObject();
        where.addProperty("id", id);

        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();

        assertThat("Found result", handler.getBodyAsList().size(), is(1));
        assertThat("Correct value", handler.getBodyAsList().get(0).get("k").getAsInt(), is(42));
    }

    @Test
    public void testExecuteProcedure() throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("arg1", 3.14159);
        params.addProperty("arg2", "xxx");
        vantiq.execute("echo", params, handler);
        waitForCompletion();

        assertThat("No Errors" + handler.getErrors(), handler.success);
        assertThat("Response body exists", handler.getBodyAsJsonObject() != null);
        assertThat("Response property exists", handler.getBodyAsJsonObject().get("arg1") != null);
        
        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg1").getAsDouble(), is(3.14159));
        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg2").getAsString(), is("xxx"));
    }

    @Test
    public void testSubscribeServiceEvent() throws Exception {
        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.SERVICES.value(), "testService/testEvent", null, callback);
        callback.waitForCompletion(5000);
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously call procedure in the service to publish the event
        JsonObject params = new JsonObject();
        vantiq.execute("testService.publishToOutbound", params, handler);
        waitForCompletion();

        // Wait for the message
        callback.waitForCompletion(5000);
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"),
                is("/services/testService/testEvent"));

        Map respBody = (Map) callback.getMessage().getBody();
        // Note: Path delivered here is the implementing event path.  The Request Id is the service event.
        assertThat("Body Path", (String) respBody.get("path"),
                is("/topics/services/testService/testEvent/publish"));
        assertThat("Event contents type", (Map) respBody.get("value"), instanceOf(Map.class));
        assertThat("Event contents value", (String) ((Map)respBody.get("value")).get("name"),
                is("outbound event"));
    }


    @Test
    public void testSubscribeTopic() throws Exception {
        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test/topic", null, callback);
        callback.waitForCompletion(5000);
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously publish to the topic
        Map body = new HashMap();
        body.put("ts", getISOString(new Date()));
        body.put("id", "SUB-" + new Date().getTime());
        VantiqResponse r = vantiq.publish("topics", "/test/topic", body);
        assertThat("Valid publish", r.isSuccess(), is(true));

        // Wait for the message
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));
    }

    @Test
    public void testSubscribeReliableTopic() throws Exception {

        Map<String,Object> topic = new HashMap<String,Object>();
        topic.put("name", "/test/topic");
        topic.put("description", "topic description");
        topic.put("isReliable", true);
        topic.put("redeliveryFrequency", 1);
        topic.put("redeliveryTTL", 100);


        VantiqResponse t = vantiq.upsert("system.topics", topic);
        assertThat("Valid insert", t.isSuccess(), is(true));
        Map<String, Object> params = new HashMap<String, Object>() ;
        params.put("persistent", true);

        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test/topic", null, callback, params);
        // These types of subscriptions may be a multi-step process where by > 1 message is required to complete the
        // connection.  callback.waitForCompletion() just waits until some message arrives, not necessarily the
        // connection message.  callback.waitForConnection() will loop (up to the specified (or default) timeout
        // period) until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
        // will return and the assertThat will detect the error state.
        //
        // Note that this is more prevalent (but not exclusive) when the path to the server is more "complicated."
        // E.g. when a proxy is involved, when running on a loaded laptop, etc.  So relatively rare, but this
        // improves the test diagnostics.  Previously, we'd just get a NPE on the callback.getMessage() if sufficient
        // time had not gone by.
        callback.waitForConnection();
        assertThat("Connected", callback.isConnected(), is(true));
        //noinspection rawtypes
        LinkedTreeMap msg = (LinkedTreeMap) callback.getMessage().getBody();
        assertThat(msg.get("subscriptionName"), instanceOf(String.class));
        assertThat(callback.getMessage().getHeaders().get("X-Request-Id"), instanceOf(String.class));
    
        // Sometimes the connection occurs during the validations above. Check for completion only if it's not connected
        if (!callback.isConnected()) {
            callback.reset();
            callback.waitForCompletion();
        }
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously publish to the topic
        Map body = new HashMap();
        body.put("ts", getISOString(new Date()));
        body.put("id", "SUBRE-" + new Date().getTime());
        VantiqResponse r = vantiq.publish("topics", "/test/topic", body);
        assertThat("Valid publish", r.isSuccess(), is(true));

        // Wait for the message
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));
        
        callback.reset();
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));
    }

    @Test
    public void testSubscribeReconnectReliableTopic() throws Exception {

        Map<String,Object> topic = new HashMap<String,Object>();
        topic.put("name", "/test/topic");
        topic.put("description", "topic description");
        topic.put("isReliable", true);
        topic.put("redeliveryFrequency", 1);
        topic.put("redeliveryTTL", 100);


        VantiqResponse t = vantiq.upsert("system.topics", topic);
        assertThat("Valid insert", t.isSuccess(), is(true));
        Map<String, Object> params = new HashMap<String, Object>() ;
        params.put("persistent", true);

        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test/topic", null, callback, params);
        // These types of subscriptions may be a multi-step process where by > 1 message is required to complete the
        // connection.  callback.waitForCompletion() just waits until some message arrives, not necessarily the
        // connection message.  callback.waitForConnection() will loop (up to the specified (or default) timeout
        // period) until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
        // will return and the assertThat will detect the error state.
        //
        // Note that this is more prevalent (but not exclusive) when the path to the server is more "complicated."
        // E.g. when a proxy is involved, when running on a loaded laptop, etc.  So relatively rare, but this
        // improves the test diagnostics.  Previously, we'd just get a NPE on the callback.getMessage() if sufficient
        // time had not gone by.
        callback.waitForConnection();
        assertThat("Connected", callback.isConnected(), is(true));
        //noinspection rawtypes
        LinkedTreeMap msg = (LinkedTreeMap) callback.getMessage().getBody();
        assertThat(msg.get("subscriptionName"), instanceOf(String.class));
        assertThat(callback.getMessage().getHeaders().get("X-Request-Id"), instanceOf(String.class));
        String ackId = msg.get("subscriptionName").toString();
        String requestId = callback.getMessage().getHeaders().get("X-Request-Id");
        
        // Sometimes the connection occurs during the validations above. Check for completion only if it's not connected
        if (!callback.isConnected()) {
            callback.reset();
            callback.waitForCompletion();
        }
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously publish to the topic
        //noinspection rawtypes
        Map body = new HashMap();
        //noinspection unchecked
        body.put("ts", getISOString(new Date()));
        //noinspection unchecked
        body.put("id", "RECONN-" + new Date().getTime());
        VantiqResponse r = vantiq.publish("topics", "/test/topic", body);
        assertThat("Valid publish", r.isSuccess(), is(true));

        // Wait for the message
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        //noinspection rawtypes
        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));

        callback.reset();
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        //noinspection rawtypes
        respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));
        
        vantiq.closeWebsocket();;

        params.put("requestId", requestId);
        params.put("subscriptionId", ackId);
        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test/topic", null, callback, params);
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));
        callback.reset();
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));
        callback.reset();
    }

    @Test
    public void testAcknowledgeReliableTopic() throws Exception {

        Map<String,Object> topic = new HashMap<String,Object>();
        topic.put("name", "/test/topic");
        topic.put("description", "topic description");
        topic.put("isReliable", true);
        topic.put("redeliveryFrequency", 5);
        topic.put("redeliveryTTL", 100);

        VantiqResponse t = vantiq.upsert("system.topics", topic);
        assertThat("Valid insert", t.isSuccess(), is(true));
        Map<String, Object> params = new HashMap<String, Object>() ;
        params.put("persistent", true);

        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), "/test/topic", null, callback, params);
        // These types of subscriptions may be a multi-step process where by > 1 message is required to complete the
        // connection.  callback.waitForCompletion() just waits until some message arrives, not necessarily the
        // connection message.  callback.waitForConnection() will loop (up to the specified (or default) timeout
        // period) until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
        // will return and the assertThat will detect the error state.
        //
        // Note that this is more prevalent (but not exclusive) when the path to the server is more "complicated."
        // E.g. when a proxy is involved, when running on a loaded laptop, etc.  So relatively rare, but this
        // improves the test diagnostics.  Previously, we'd just get a NPE on the callback.getMessage() if sufficient
        // time had not gone by.
        callback.waitForConnection();
        assertThat("Connected", callback.isConnected(), is(true));
        //noinspection rawtypes
        LinkedTreeMap msg = (LinkedTreeMap) callback.getMessage().getBody();
        assertThat(msg.get("subscriptionName"), instanceOf(String.class));
        assertThat(callback.getMessage().getHeaders().get("X-Request-Id"), instanceOf(String.class));
        String ackId = msg.get("subscriptionName").toString();
        String requestId = callback.getMessage().getHeaders().get("X-Request-Id");
        
        // Sometimes the connection occurs during the validations above. Check for completion only if it's not connected
        if (!callback.isConnected()) {
            callback.reset();
            callback.waitForCompletion();
        }
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously publish to the topic
        //noinspection rawtypes
        Map body = new HashMap();
        //noinspection unchecked
        body.put("ts", getISOString(new Date()));
        //noinspection unchecked
        body.put("id", "ACK-" + new Date().getTime());
        VantiqResponse r = vantiq.publish("topics", "/test/topic", body);
        assertThat("Valid publish", r.isSuccess(), is(true));

        // Wait for the message
        callback.waitForCompletion();
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/topics/test/topic"));

        //noinspection rawtypes
        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/topics/test/topic/publish"));

        callback.reset();
        vantiq.ack(ackId, requestId, respBody);
        waitForCompletion();
        HashMap<String,String> where = new HashMap<String,String>();
        where.put("subscriptionName", ackId);
        
        vantiq.select("ArsEventAcknowledgement", null, where, null);
    }
    
    @Test
    @Ignore("Setup JSONPlaceholder polling Source and run this test explicitly")
    public void testSubscribeSource() throws Exception {
        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to source
        vantiq.subscribe(Vantiq.SystemResources.SOURCES.value(), "JSONPlaceholder", null, callback);
        callback.waitForCompletion();
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Wait for the message
        callback.waitForCompletion(5000);
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/sources/JSONPlaceholder"));

        //noinspection rawtypes
        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), is("/sources/JSONPlaceholder/receive"));
    }

    @Test
    public void testSubscribeType() throws Exception {
        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to type
        vantiq.subscribe(Vantiq.SystemResources.TYPES.value(), "TestType", Vantiq.TypeOperation.INSERT, callback);
        callback.waitForCompletion();
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously insert record
        Map<String,Object> record = new HashMap<String,Object>();
        record.put("id", "SUB-" + (new Date()).getTime());
        record.put("ts", "2016-10-26T23:22:12Z");
        record.put("x", 3.14159);
        record.put("k", 42);

        Map<String,Integer> inner = new HashMap<String,Integer>();
        inner.put("a", 1);
        inner.put("b", 2);
        record.put("o", inner);

        VantiqResponse r = vantiq.insert("TestType", record);
        assertThat("Valid insert", r.isSuccess(), is(true));

        // Wait for the message
        callback.waitForCompletion(5000);
        assertThat("Message received", callback.hasFired(), is(true));
        assertThat("Request Id", callback.getMessage().getHeaders().get("X-Request-Id"), is("/types/TestType/insert"));
    
        //noinspection rawtypes
        Map respBody = (Map) callback.getMessage().getBody();
        assertThat("Body Path", (String) respBody.get("path"), startsWith("/types/TestType/insert"));
    }

    @Test
    public void testUploadAndDownload() throws Exception {
        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        vantiq.upload(file, "text/plain", fileName, handler);
        waitForCompletion();

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is("text/plain"));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/docs/" + fileName));

        // Get the file content and ensure it's correct
        handler.reset();
        vantiq.selectOne("system.documents", fileName, handler);
        waitForCompletion();

        // Ensure the succeeded
        assertTrue("Select succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is("text/plain"));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/docs/" + fileName));

        // Download the file and check the content
        handler.reset();
        vantiq.download("/docs/" + fileName, handler);
        waitForCompletion();

        assertThat("Download succeeded",   handler.success);
        assertThat("Correct content type", handler.getContentType(), is("text/plain"));

        BufferedSource source = ((BufferedSource) handler.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }

    @Test
    public void testUploadAndDownloadSync() throws Exception {
        String fileName = "testFile.txt";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        VantiqResponse response = vantiq.upload(file, "text/plain", fileName);

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is("text/plain"));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/docs/" + fileName));

        // Get the file content and ensure it's correct
        response = vantiq.selectOne("system.documents", fileName);

        // Ensure the succeeded
        assertTrue("Select succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is("text/plain"));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/docs/" + fileName));

        // Download the file and check the content
        response = vantiq.download("/docs/" + fileName);
        assertThat("Download succeeded", response.isSuccess());
        assertThat("Correct content type", response.getContentType(), is("text/plain"));

        BufferedSource source = ((BufferedSource) response.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }

    //------------------------------------------------------------------------------------
    // Integration tests for previously untested API surface
    //------------------------------------------------------------------------------------

    @Test
    public void testSystemResourcesRecognizedByServer() throws Exception {
        // Every resource the SDK advertises must be accepted by the server.  A removed
        // resource comes back as "resource.unknown" or "support ... has not been installed".
        for (Vantiq.SystemResources r : Vantiq.SystemResources.values()) {
            VantiqResponse response = vantiq.count("system." + r.value(), null);
            assertTrue("Resource " + r.value() + " accepted by server: " + response, response.isSuccess());
        }
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
        String typeQuery = "where=" + URLEncoder.encode("{\"name\":\"TestType\"}", "UTF-8")
                + "&props=" + URLEncoder.encode("[\"name\"]", "UTF-8");
        String procQuery = "where=" + URLEncoder.encode("{\"name\":\"echo\"}", "UTF-8")
                + "&props=" + URLEncoder.encode("[\"name\"]", "UTF-8");

        JsonArray requests = new JsonArray();
        requests.add(batchRequest("GET", "/types?" + typeQuery));
        requests.add(batchRequest("GET", "/procedures?" + procQuery));

        VantiqResponse response = vantiq.batch(requests);
        assertTrue("Batch succeeded: " + response, response.isSuccess());

        @SuppressWarnings("unchecked")
        List<JsonObject> results = (List<JsonObject>) response.getBody();
        assertThat("One result per request", results.size(), is(2));

        assertThat("Type lookup status", results.get(0).get("status").getAsInt(), is(200));
        assertThat("Type lookup body",
                   results.get(0).getAsJsonArray("body").get(0).getAsJsonObject().get("name").getAsString(),
                   is("TestType"));
        assertThat("Procedure lookup status", results.get(1).get("status").getAsInt(), is(200));
        assertThat("Procedure lookup body",
                   results.get(1).getAsJsonArray("body").get(0).getAsJsonObject().get("name").getAsString(),
                   is("echo"));
    }

    @Test
    public void testGetNamespaceUsers() throws Exception {
        // Discover the current namespace from a record we know exists
        JsonObject where = new JsonObject();
        where.addProperty("name", "TestType");
        VantiqResponse response = vantiq.select("system.types", Arrays.asList("ars_namespace"), where, null);
        assertTrue("Type lookup succeeded: " + response, response.isSuccess());
        @SuppressWarnings("unchecked")
        String namespace = ((List<JsonObject>) response.getBody()).get(0).get("ars_namespace").getAsString();

        response = vantiq.getNamespaceUsers(namespace);
        assertTrue("getNamespaceUsers succeeded: " + response, response.isSuccess());

        JsonArray users = (JsonArray) response.getBody();
        assertThat("At least one authorized user", users.size(), greaterThan(0));
        for (int i = 0; i < users.size(); i++) {
            JsonObject user = users.get(i).getAsJsonObject();
            assertThat("username present", user.get("username"), notNullValue());
            assertThat("preferredUsername present", user.get("preferredUsername"), notNullValue());
        }
    }

    @Test
    public void testRawGet() throws Exception {
        VantiqResponse response = vantiq.rawGet("api/v1/_status", null, null);
        assertTrue("rawGet succeeded: " + response, response.isSuccess());
        JsonObject status = (JsonObject) response.getBody();
        assertThat("Server build info returned", status.get("buildInfo"), notNullValue());

        // Query parameters are passed through on the raw path
        Map<String,String> queryParams = new HashMap<String,String>();
        queryParams.put("where", "{\"name\":\"TestType\"}");
        queryParams.put("props", "[\"name\"]");
        response = vantiq.rawGet("api/v1/resources/types", queryParams, null);
        assertTrue("rawGet with params succeeded: " + response, response.isSuccess());
        JsonArray types = (JsonArray) response.getBody();
        assertThat("Single type", types.size(), is(1));
        assertThat("Correct type", types.get(0).getAsJsonObject().get("name").getAsString(), is("TestType"));
    }

    @Test
    public void testRawPostPutDelete() throws Exception {
        Date now = new Date();
        String id = "RAW-" + now.getTime();

        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 1.5);
        record.addProperty("k", 7);

        // Insert using the raw POST
        VantiqResponse response = vantiq.post("/resources/custom/TestType", record);
        assertTrue("POST succeeded: " + response, response.isSuccess());
        String objectId = ((JsonObject) response.getBody()).get("_id").getAsString();

        // Update using the raw PUT
        JsonObject update = new JsonObject();
        update.addProperty("k", 99);
        response = vantiq.put("/resources/custom/TestType/" + objectId, update);
        assertTrue("PUT succeeded: " + response, response.isSuccess());

        response = vantiq.selectOne("TestType", objectId);
        assertTrue("Select after PUT succeeded: " + response, response.isSuccess());
        assertThat("Updated value", ((JsonObject) response.getBody()).get("k").getAsInt(), is(99));

        // Remove using the raw DELETE; the server answers 204 with no body
        response = vantiq.del("/resources/custom/TestType/" + objectId);
        assertTrue("DELETE succeeded: " + response, response.isSuccess());
        assertThat("DELETE status", response.getStatusCode(), is(204));
        assertThat("DELETE body", (Boolean) response.getBody(), is(true));

        response = vantiq.selectOne("TestType", objectId);
        assertThat("Record gone after DELETE", response.isSuccess(), is(false));
    }

    @Test
    public void testRefreshAndRevoke() throws Exception {
        // A token obtained with username/password can be refreshed and revoked without
        // touching the long-lived token this test run may have been given.
        // Gradle passes unset properties as empty strings, so check for both null and empty
        assumeTrue("Requires username and password properties",
                   username != null && !username.isEmpty() && password != null && !password.isEmpty());

        Vantiq v = new Vantiq(server);
        VantiqResponse response = v.authenticate(username, password);
        assertTrue("Authenticated: " + response, response.isSuccess());
        String original = v.getAccessToken();
        assertThat("Access token issued", original, notNullValue());
        assertThat("idToken issued", v.getIdToken(), notNullValue());

        response = v.refresh(original);
        assertTrue("Refresh succeeded: " + response, response.isSuccess());
        String refreshed = v.getAccessToken();
        assertThat("Refreshed token usable", v.count("system.types", null).isSuccess(), is(true));

        response = v.revoke(refreshed);
        assertTrue("Revoke succeeded: " + response, response.isSuccess());
        assertThat("Session no longer authenticated", v.isAuthenticated(), is(false));

        Vantiq stale = new Vantiq(server);
        stale.setAccessToken(refreshed);
        response = stale.count("system.types", null);
        assertThat("Revoked token rejected", response.isSuccess(), is(false));
        assertThat("Revoked token status", response.getStatusCode(), is(401));
    }
}
