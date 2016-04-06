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

    public SortSpec(String property) {
        this(property, /*descending=*/ false);
    }

    public SortSpec(String property, boolean descending) {
        this.property = property;
        this.descending = descending;
    }

    public String getProperty() {
        return this.property;
    }

    public boolean isDescending() {
        return this.descending;
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty(this.property, (this.descending ? -1 : 1));
        return obj;
    }

    /**
     * Serializes List of SortSpec objects into a JsonArray
     */
    public static JsonArray serialize(List<SortSpec> sortSpecs) {
        JsonArray array = new JsonArray();
        for(SortSpec sort : sortSpecs) {
            array.add(sort.serialize());
        }
        return array;
    }

}
