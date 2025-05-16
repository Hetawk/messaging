package com.andgroupco.messaging.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.andgroupco.messaging.models.Contact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for parsing CSV and Excel files
 */
public class FileParserUtils {
    private static final String TAG = "FileParserUtils";

    /**
     * Parse contacts from a URI (CSV or Excel)
     * 
     * @param context  application context
     * @param uri      file URI
     * @param mimeType file MIME type
     * @return list of parsed contacts
     */
    public static List<Contact> parseContactsFromUri(Context context, Uri uri, String mimeType) throws IOException {
        if (mimeType == null) {
            throw new IllegalArgumentException("MIME type cannot be null");
        }

        if (mimeType.equals("text/csv") || mimeType.equals("text/comma-separated-values")) {
            return parseCsvFile(context, uri);
        } else if (mimeType.equals("application/vnd.ms-excel") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            // For Excel files, we'll use Apache POI or other library
            // But for simplicity, we'll assume Excel files are in CSV format for this
            // example
            return parseCsvFile(context, uri);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    /**
     * Parse a CSV file
     * 
     * @param context application context
     * @param uri     file URI
     * @return list of parsed contacts
     */
    private static List<Contact> parseCsvFile(Context context, Uri uri) throws IOException {
        List<Contact> contacts = new ArrayList<>();
        List<String> headers = null;

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Parse CSV line
                List<String> values = parseCsvLine(line);

                // First line is headers
                if (lineNum == 1) {
                    headers = values;
                    continue;
                }

                // Create contact from values
                if (headers != null && !values.isEmpty()) {
                    Contact contact = new Contact();

                    // Map values to fields based on headers
                    for (int i = 0; i < Math.min(headers.size(), values.size()); i++) {
                        String header = headers.get(i);
                        String value = values.get(i);

                        if (header != null && !header.isEmpty()) {
                            contact.setField(header, value);
                        }
                    }

                    contacts.add(contact);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error parsing CSV file", e);
            throw e;
        }

        return contacts;
    }

    /**
     * Parse a CSV line into list of values
     * Handles quoted values with commas inside
     * 
     * @param line CSV line
     * @return list of values
     */
    public static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Toggle inQuotes flag
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                // End of value
                result.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                // Add character to current value
                currentValue.append(c);
            }
        }

        // Add the last value
        result.add(currentValue.toString().trim());

        return result;
    }
}
