package io.vantiq.client;

import java.util.List;

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

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List getParams() {
        return params;
    }

    public String toString() {
        return code + ": " + message;
    }
}
