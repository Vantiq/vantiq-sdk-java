package io.vantiq.client;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Class used to represent a sort specification for a select query.
 */
public class SortSpec {

    private String property;
    private boolean descending = false;

    public SortSpec(String property, boolean descending) {
        this.property = property;
        this.descending = descending;
    }

    /**
     * The property name used for the sort
     * @return The sort property name
     */
    public String getProperty() {
        return this.property;
    }

    /**
     * Indicates if the sort order should be ascending or descending.
     * @return true for a descending sort
     */
    public boolean isDescending() {
        return this.descending;
    }

    /**
     * Returns the JsonObject that can be serialized by {@link com.google.gson.Gson Gson}
     * @return The JsonObject represented by this sort specification
     */
    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty(this.property, (this.descending ? -1 : 1));
        return obj;
    }

}
