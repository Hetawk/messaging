package com.andgroupco.messaging.models;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A flexible contact model that can store any field structure
 */
public class Contact {
    private Map<String, String> fields = new HashMap<>();
    private long id;

    public Contact() {
        // Default constructor
    }

    /**
     * Set a field value
     * 
     * @param key   field name
     * @param value field value
     */
    public void setField(String key, String value) {
        if (value == null) {
            value = "";
        }
        fields.put(key, value);
    }

    /**
     * Get a field value
     * 
     * @param key field name
     * @return field value or empty string if field doesn't exist
     */
    public String getField(String key) {
        return fields.getOrDefault(key, "");
    }

    /**
     * Get all field keys
     * 
     * @return set of all field keys
     */
    public Set<String> getFieldKeys() {
        return fields.keySet();
    }

    /**
     * Get all fields
     * 
     * @return map of all fields
     */
    public Map<String, String> getAllFields() {
        return new HashMap<>(fields);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Check if any field contains the search query
     * 
     * @param query search query
     * @return true if any field contains the query
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();
        for (String value : fields.values()) {
            if (value != null && value.toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return "Contact{fields=" + fields + "}";
    }
}
