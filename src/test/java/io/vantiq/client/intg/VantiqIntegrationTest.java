package io.vantiq.client.intg;

import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import io.vantiq.client.*;
import okio.*;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

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
        vantiq = new Vantiq(server);
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
        // (onTestPublish.vail) will fire.  Thus, this test has pretty much the same content as testPublishTopic()
        // below -- just the mechanism by which the "publish" action happens is different.
        vantiq.publish(Vantiq.SystemResources.SERVICES.value(), "testService/inboundTestEvent", message, handler);
        waitForCompletion();
        assertTrue("Publish to Svc Event succeeded", handler.success);

        // Rule should insert the record into TestType
        // so select it to find the record.  However, this may take time so, adding slight delay.
        Thread.sleep(500);

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

        // Publish to topic
        vantiq.publish("topics", "/test/topic", message, handler);
        waitForCompletion();
        assertTrue("Publish succeeded", handler.success);

        // Rule should insert the record into TestType
        // so select it to find the record.  However, this may take time so, adding slight delay.
        Thread.sleep(500);

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

        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg1").getAsDouble(), is(3.14159));
        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg2").getAsString(), is("xxx"));
    }

    @Test
    public void testSubscribeServiceEvent() throws Exception {
        UnitTestSubscriptionCallback callback = new UnitTestSubscriptionCallback();

        // Subscribe to topic
        vantiq.subscribe(Vantiq.SystemResources.SERVICES.value(), "testService/testEvent", null, callback);
        callback.waitForCompletion();
        assertThat("Connected", callback.isConnected(), is(true));
        callback.reset();

        // Synchronously call procedure in the service to publish the event
        JsonObject params = new JsonObject();
        vantiq.execute("testService.publishToOutbound", params, handler);
        waitForCompletion();

        // Wait for the message
        callback.waitForCompletion();
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
        callback.waitForCompletion();
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
        // period until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
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
        // period until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
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
        // period until it sees that the connection status is satisfied.  If not (that is, timeout occurs), then it
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

    @Test
    public void testUploadAndDownloadJPG() throws Exception {
        String fileName = "testImage.jpg";
        testUploadAndDownloadImageHelper(fileName, "image/jpeg");
    }

    @Test
    public void testUploadAndDownloadPNG() throws Exception {
        String fileName = "testImage.png";
        testUploadAndDownloadImageHelper(fileName, "image/png");
    }

    public void testUploadAndDownloadImageHelper(String fileName, String contentType) throws Exception {
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        String resourcePath = "/resources/" + Vantiq.SystemResources.IMAGES.value();
        vantiq.upload(file, contentType, fileName, resourcePath, handler);
        waitForCompletion();

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is(contentType));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/pics/" + fileName));

        // Get the file content and ensure it's correct
        handler.reset();
        vantiq.selectOne("system.images", fileName, handler);
        waitForCompletion();

        // Ensure the succeeded
        assertTrue("Select succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is(contentType));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/pics/" + fileName));

        // Download the file and check the content
        handler.reset();
        vantiq.download("/pics/" + fileName, handler);
        waitForCompletion();

        assertThat("Download succeeded",   handler.success);
        assertThat("Correct content type", handler.getContentType(), is(contentType));

        BufferedSource source = ((BufferedSource) handler.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }

    @Test
    public void testUploadAndDownloadJPGSync() throws Exception {
        String fileName = "testImage.jpg";
        testUploadAndDownloadImageSyncHelper(fileName, "image/jpeg");
    }

    @Test
    public void testUploadAndDownloadPNGSync() throws Exception {
        String fileName = "testImage.png";
        testUploadAndDownloadImageSyncHelper(fileName, "image/png");
    }

    public void testUploadAndDownloadImageSyncHelper(String fileName, String contentType) throws Exception {
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        String resourcePath = "/resources/" + Vantiq.SystemResources.IMAGES.value();
        VantiqResponse response = vantiq.upload(file, contentType, fileName, resourcePath);

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is(contentType));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/pics/" + fileName));

        // Get the file content and ensure it's correct
        response = vantiq.selectOne("system.images", fileName);

        // Ensure the succeeded
        assertTrue("Select succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is(contentType));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/pics/" + fileName));

        // Download the file and check the content
        response = vantiq.download("/pics/" + fileName);
        assertThat("Download succeeded", response.isSuccess());
        assertThat("Correct content type", response.getContentType(), is(contentType));

        BufferedSource source = ((BufferedSource) response.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }

    @Test
    public void testUploadAndDownloadVideo() throws Exception {
        String fileName = "testVideo.mp4";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        String resourcePath = "/resources/" + Vantiq.SystemResources.VIDEOS.value();
        vantiq.upload(file, "video/mp4", fileName, resourcePath, handler);
        waitForCompletion();

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is("video/mp4"));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/vids/" + fileName));

        // Get the file content and ensure it's correct
        handler.reset();
        vantiq.selectOne("system.videos", fileName, handler);
        waitForCompletion();

        // Ensure the succeeded
        assertTrue("Select succeeded", handler.success);
        assertThat("Correct name",     ((JsonObject) handler.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) handler.getBody()).get("fileType").getAsString(), is("video/mp4"));
        assertThat("Correct content",  ((JsonObject) handler.getBody()).get("content").getAsString(), is("/vids/" + fileName));

        // Download the file and check the content
        handler.reset();
        vantiq.download("/vids/" + fileName, handler);
        waitForCompletion();

        assertThat("Download succeeded",   handler.success);
        assertThat("Correct content type", handler.getContentType(), is("video/mp4"));

        BufferedSource source = ((BufferedSource) handler.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }

    @Test
    public void testUploadAndDownloadVideoSync() throws Exception {
        String fileName = "testVideo.mp4";
        File file = new File(this.getClass().getResource("/" + fileName).getFile());
        String resourcePath = "/resources/" + Vantiq.SystemResources.VIDEOS.value();
        VantiqResponse response = vantiq.upload(file, "video/mp4", fileName, resourcePath);

        // Ensure the file uploaded successfully
        assertTrue("Upload succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is("video/mp4"));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/vids/" + fileName));

        // Get the file content and ensure it's correct
        response = vantiq.selectOne("system.videos", fileName);

        // Ensure the succeeded
        assertTrue("Select succeeded", response.isSuccess());
        assertThat("Correct name",     ((JsonObject) response.getBody()).get("name").getAsString(), is(fileName));
        assertThat("Correct fileType", ((JsonObject) response.getBody()).get("fileType").getAsString(), is("video/mp4"));
        assertThat("Correct content",  ((JsonObject) response.getBody()).get("content").getAsString(), is("/vids/" + fileName));

        // Download the file and check the content
        response = vantiq.download("/vids/" + fileName);
        assertThat("Download succeeded", response.isSuccess());
        assertThat("Correct content type", response.getContentType(), is("video/mp4"));

        BufferedSource source = ((BufferedSource) response.getBody());
        assertThat("Correct file content", source.readByteArray(), is(Files.readAllBytes(file.toPath())));
    }
}
