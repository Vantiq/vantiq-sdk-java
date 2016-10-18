package io.vantiq.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Base class for unit tests that provide mocking support for OkHttp
 * and Retrofit.
 */
public class VantiqTestBase {

    public final static String USERNAME = "joe";
    public final static String PASSWORD = "no-one-will-guess";

    protected MockWebServer server;

    protected final UnitTestResponseHandler handler;
    protected final Gson gson = new Gson();

    protected VantiqTestBase() {
        this(true);
    }

    protected VantiqTestBase(boolean useHandler) {
        if(useHandler) {
            handler = new UnitTestResponseHandler();
        } else {
            handler = null;
        }
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        if(handler != null) handler.reset();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server = null;
    }

    protected String toJson(Object values) {
        if(values instanceof List) {
            String v = gson.toJson(values, new TypeToken<List>(){}.getType());
            return v;
        } else if(values instanceof Map) {
            return gson.toJson(values, new TypeToken<Map>(){}.getType());
        }
        throw new IllegalArgumentException("Invalid values type: " + values);
    }

    protected void waitForCompletion() throws InterruptedException {
        synchronized (handler) {
            handler.wait(2000);
        }
    }

    public class JsonArrayBuilder {

        private JsonArray arr = new JsonArray();

        public JsonArrayBuilder add(JsonObject obj) {
            this.arr.add(obj);
            return this;
        }

        public JsonArray array() {
            return this.arr;
        }

        public String json() {
            return gson.toJson(this.arr);
        }
    }

    public class JsonObjectBuilder {

        private JsonObject obj = new JsonObject();

        public JsonObjectBuilder addProperty(String name, String value) {
            this.obj.addProperty(name, value);
            return this;
        }

        public JsonObjectBuilder addProperty(String name, Number value) {
            this.obj.addProperty(name, value);
            return this;
        }

        public JsonObject obj() {
            return this.obj;
        }

        public String json() {
            return gson.toJson(this.obj);
        }

    }

    public String readAll(Buffer body) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream((int) body.size());
        body.copyTo(os);
        return new String(os.toByteArray());
    }

}
