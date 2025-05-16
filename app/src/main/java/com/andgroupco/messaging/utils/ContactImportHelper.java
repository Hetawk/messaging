package com.andgroupco.messaging.utils;

import android.content.Context;
import android.util.Log;

import com.andgroupco.messaging.db.ContactDbHelper;
import com.andgroupco.messaging.models.Contact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for importing contacts from sample data
 */
public class ContactImportHelper {
    private static final String TAG = "ContactImportHelper";

    /**
     * Import contacts from the included sample data file
     * 
     * @param context application context
     * @return number of contacts imported
     */
    public static int importSampleContacts(Context context) {
        ContactDbHelper dbHelper = new ContactDbHelper(context);
        List<Contact> contacts = new ArrayList<>();

        try (InputStream inputStream = context.getAssets().open("JICF_Database.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            List<String> headers = null;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Parse CSV line
                List<String> values = FileParserUtils.parseCsvLine(line);

                // First line is headers
                if (lineNum == 1) {
                    headers = values;
                    continue;
                }

                // Skip incomplete rows
                if (values.size() < 3) {
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

            // Save contacts to database
            int importCount = 0;
            if (!contacts.isEmpty()) {
                importCount = dbHelper.saveContacts(contacts);
                Log.d(TAG, "Imported " + importCount + " contacts from sample data");
            }

            return importCount;

        } catch (IOException e) {
            Log.e(TAG, "Error importing sample contacts", e);
            return 0;
        }
    }
}
