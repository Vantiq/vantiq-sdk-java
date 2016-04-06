package io.vantiq.client;

import java.util.List;

/**
 * This class represents an error during a REST call to a Vantiq server.
 */
public class VantiqError {

    private String code;
    private String message;
    private List params;

    // Private empty constructor required by Gson
    private VantiqError() {}

    public VantiqError(String code, String message, List params) {
        this.code = code;
        this.message = message;
        this.params = params;
    }

    /**
     * The unique code that identifies the error.
     *
     * @return The error code
     */
    public String getCode() {
        return code;
    }

    /**
     * The human-readable string describing the error.
     *
     * @return The error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * The parameters associated with the error.
     *
     * @return parameters associated with the error
     */
    public List getParams() {
        return params;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
