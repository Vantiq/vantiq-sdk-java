package io.vantiq.client;

import java.util.Map;

/**
 * This class represents a single subscription messages from Vantiq
 */
public class SubscriptionMessage {

    private int status;
    private String contentType;
    private Map<String, String> headers;
    private Object body;

    /**
     * The HTTP status code for this message.  Usually, this is 100.
     */
    public int getStatus() {
        return status;
    }

    /**
     * The content type for the body of the message.  Usually, this is
     * application/json indicating the content was JSON encoded.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The headers associated with the response.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the payload for the message.  For a JSON encoded
     * message, this would be a Map.
     */
    public Object getBody() {
        return body;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SubscriptionMessage[\n");
        sb.append("  status:").append(this.status).append('\n');
        sb.append("  contentType:").append(this.contentType).append('\n');
        sb.append("  headers:").append(this.headers).append('\n');
        sb.append("  body:").append(this.body).append('\n');
        sb.append(']');
        return sb.toString();
    }
}
