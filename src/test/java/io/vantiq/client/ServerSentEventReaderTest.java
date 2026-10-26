package io.vantiq.client;

import com.google.gson.JsonElement;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for parsing text/event-stream responses.
 */
public class ServerSentEventReaderTest {

    private BufferedSource source(String text) {
        return new Buffer().writeUtf8(text);
    }

    @Test
    public void testReadAll() throws Exception {
        List<JsonElement> events = ServerSentEventReader.readAll(source("data: {\"i\":1}\n\ndata: {\"i\":2}\n\n"));
        assertThat("Two events", events.size(), is(2));
        assertThat("First", events.get(0).getAsJsonObject().get("i").getAsInt(), is(1));
        assertThat("Second", events.get(1).getAsJsonObject().get("i").getAsInt(), is(2));
    }

    @Test
    public void testScalarsAndMissingTrailingBlankLine() throws Exception {
        List<JsonElement> events = ServerSentEventReader.readAll(source("data: 11\n\ndata: \"x\""));
        assertThat("Two events", events.size(), is(2));
        assertThat("Number", events.get(0).getAsInt(), is(11));
        assertThat("String", events.get(1).getAsString(), is("x"));
    }

    @Test
    public void testMultiLineDataAndComments() throws Exception {
        // Multiple data lines in one event are joined with newlines; comments and other fields are ignored
        List<JsonElement> events = ServerSentEventReader.readAll(source(": keep-alive\nevent: x\ndata: [1,\ndata: 2]\n\n"));
        assertThat("One event", events.size(), is(1));
        assertThat("Joined payload", events.get(0).getAsJsonArray().size(), is(2));
    }

    @Test
    public void testEmptyStream() throws Exception {
        assertThat("No events", ServerSentEventReader.readAll(source("")).size(), is(0));
    }

    @Test
    public void testIncrementalHandler() throws Exception {
        final List<Integer> seen = new ArrayList<Integer>();
        ServerSentEventReader.read(source("data: 1\n\ndata: 2\n\ndata: 3\n\n"), new ServerSentEventReader.EventHandler() {
            @Override
            public void onEvent(JsonElement data) {
                seen.add(data.getAsInt());
            }
        });
        assertThat("All delivered", seen.size(), is(3));
        assertThat("In order", seen.get(2), is(3));
    }
}
