package io.vantiq.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okio.BufferedSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a <code>text/event-stream</code> response produced by a streamed procedure
 * execution (see {@link Vantiq#executeStreaming(String, Object, Map, boolean, ResponseHandler)}).
 * The server emits one event per result element as <code>data: &lt;json&gt;</code> followed
 * by a blank line, with no event names or ids.  The stream ends when the response ends.
 */
public class ServerSentEventReader {

    /**
     * Receives each event's data as it is read.
     */
    public interface EventHandler {
        /**
         * Called once per event.
         *
         * @param data The event payload, parsed as JSON
         */
        void onEvent(JsonElement data);
    }

    private ServerSentEventReader() {}

    /**
     * Reads every event from the stream and returns their payloads.  The source is
     * closed when the stream ends.
     *
     * @param source The response body
     * @return The parsed payload of each event, in order
     * @throws IOException If the stream cannot be read
     */
    public static List<JsonElement> readAll(BufferedSource source) throws IOException {
        final List<JsonElement> events = new ArrayList<JsonElement>();
        read(source, new EventHandler() {
            @Override
            public void onEvent(JsonElement data) {
                events.add(data);
            }
        });
        return events;
    }

    /**
     * Reads events from the stream, handing each to the handler as soon as it is complete.
     * The source is closed when the stream ends.
     *
     * @param source The response body
     * @param handler Receives each event
     * @throws IOException If the stream cannot be read
     */
    public static void read(BufferedSource source, EventHandler handler) throws IOException {
        StringBuilder data = new StringBuilder();
        boolean haveData = false;
        try {
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if (line.isEmpty()) {
                    // Blank line terminates an event
                    if (haveData) {
                        handler.onEvent(parse(data.toString()));
                        data.setLength(0);
                        haveData = false;
                    }
                } else if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) {
                        value = value.substring(1);
                    }
                    if (haveData) {
                        data.append('\n');
                    }
                    data.append(value);
                    haveData = true;
                }
                // Comment lines (":...") and other fields are ignored
            }
            // Tolerate a final event with no trailing blank line
            if (haveData) {
                handler.onEvent(parse(data.toString()));
            }
        } finally {
            source.close();
        }
    }

    private static JsonElement parse(String data) {
        return JsonParser.parseString(data);
    }
}
