package io.vantiq.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the response from Vantiq from a single operation.
 */
public class VantiqResponse {

    private static final JsonParser parser = new JsonParser();
    private static final Gson gson = new Gson();

    private int count = -1;
    private int statusCode = -1;
    private String contentType;
    private Object body;
    private Throwable exception;
    private List<VantiqError> errors;

    private Response response;

    public static VantiqResponse createFromResponse(Response response, boolean isStreamingResponse) {
        try {
            if (response.isSuccessful()) {
                return new VantiqResponse(VantiqResponse.extractBody(response, isStreamingResponse), response);
            } else {
                return new VantiqResponse(VantiqResponse.extractErrors(response), response);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return new VantiqResponse(ex);
        }
    }

    public VantiqResponse(Object body, Response response) {
        this.response = response;

        // Check for count in header
        String cntValue = response.header("X-Total-Count");
        if (cntValue != null) {
            try {
                this.count = Integer.parseInt(cntValue);
            } catch (NumberFormatException ex) {
                /* If not parsable, then just ignore it */
            }
        }

        // Extract content type
        this.contentType = response.header("Content-Type");

        // Extract HTTP status code
        this.statusCode = response.code();

        // Extract body
        this.body = body;
    }

    public VantiqResponse(List<VantiqError> errors, Response response) {
        this.response = response;

        // Extract content type
        this.contentType = response.header("Content-Type");

        // Extract HTTP status code
        this.statusCode = response.code();

        // Store errors
        this.errors = errors;
    }

    public VantiqResponse(Throwable exception) {
        this.exception = exception;
    }

    //------------------------------------------------------------------------
    // Helper methods
    //------------------------------------------------------------------------

    protected void setBody(Object body) {
        this.body = body;
    }

    public static Object extractBody(Response response, boolean isStreamingResponse) throws IOException {
        Object body = null;

        if(isStreamingResponse) {
            body = response.body().source();
        } else {
            String contentType = response.header("Content-Type");

            if ("application/json".equals(contentType)) {
                String stringBody = response.body().string();
                if (stringBody != null && stringBody.length() > 0) {
                    body = parser.parse(stringBody);
                }
            } else if (contentType != null && contentType.startsWith("text/")) {
                body = response.body().string();
            } else {
                // If not text, then just return the bytes
                body = response.body().bytes();
            }
        }

        return body;
    }

    public static List<VantiqError> extractErrors(Response response) throws IOException {
        String body = response.body().string();
        if(body != null) body = body.trim();

        if(body.startsWith("[")) {
            Type errorsType = new TypeToken<List<VantiqError>>(){}.getType();
            List<VantiqError> listOfErrors;
            
            try
            {
                listOfErrors = gson.fromJson(body, errorsType);
            }
            catch (Exception ex)
            {
                //System.err.println("Exception parsing '" + body + "'");
                //System.err.println(ex.toString());

                String code = "io.vantiq.nonjson.error";
                String message = body + " (" + response.code() + ")";

                VantiqError ve = new VantiqError(code,message,null);

                listOfErrors = new ArrayList<>();
                listOfErrors.add(ve);
            }
            
            return listOfErrors;
        } 
        else 
        {
            VantiqError ve;

            try
            {
                ve = gson.fromJson(body, VantiqError.class);
            }
            catch (Exception ex)
            {
                //System.err.println("Exception parsing '" + body + "'");
                //System.err.println(ex.toString());

                String code = "io.vantiq.nonjson.error";
                String message = body + " (" + response.code() + ")";

                ve = new VantiqError(code,message,null);
            }

            return Collections.singletonList(ve);
        }
    }

    /**
     * Returns the underlying response object
     *
     * @return The raw response from the Vantiq server
     */
    public Response getResponse() {
        return this.response;
    }

    /**
     * Returns true if the call was successfully, meaning there were no errors
     * or exceptions
     *
     * @return Boolean indicating if the call was successful.
     */
    public boolean isSuccess() {
        return this.response.isSuccessful();
    }

    /**
     * Returns true if at least one error was returned from the server
     * @return true if at least one error exists
     */
    public boolean hasErrors() {
        return this.errors != null && this.errors.size() > 0;
    }

    /**
     * Returns true if an exception occurred during the client processing
     * @return true if an exception was thrown
     */
    public boolean hasException() {
        return this.exception != null;
    }

    /**
     * Returns the exception if {@link #hasException} returned true
     * @return The exception that was thrown or null
     */
    public Throwable getException() {
        return this.exception;
    }

    /**
     * Returns the errors if {@link #hasErrors} returned true
     * @return The errors returned from the Vantiq server or null
     */
    public List<VantiqError> getErrors() {
        return this.errors;
    }

    /**
     * Returns the count from the "X-Total-Count" header.  If the
     * header wasn't present, then the count returns -1.
     * @return The returned count or -1 if no count header was present.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Returns the content type from the "Content-Type" HTTP response
     * header.
     * @return The content type of the response
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the HTTP status code returned from the response
     * @return The HTTP status code for the response
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the response body.  If there was an exception or error, then
     * no body will exist.
     * @return The parsed response body
     */
    public Object getBody() {
        return this.body;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("VantiqResponse[\n");
        sb.append("  statusCode:").append(this.statusCode).append('\n');
        sb.append("  contentType:").append(this.contentType).append('\n');
        if(this.count > -1) {
            sb.append("  count:").append(this.count).append('\n');
        }
        if(this.errors != null) {
            sb.append("  errors:").append(this.errors).append('\n');
        }
        if(this.exception != null) {
            sb.append("  exception:").append(this.exception.getMessage()).append('\n');
        }
        if(this.body != null) {
            sb.append("  body:").append(this.body).append('\n');
        }
        sb.append(']');
        return sb.toString();
    }
}
