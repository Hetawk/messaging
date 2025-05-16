package com.andgroupco.messaging.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.andgroupco.messaging.models.Contact;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContactDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "ContactDbHelper";
    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 1;

    // Table structure - using JSON to store variable fields
    private static final String CREATE_CONTACTS_TABLE = "CREATE TABLE contacts (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "json_data TEXT NOT NULL, " +
            "search_index TEXT NOT NULL)";

    public ContactDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_CONTACTS_TABLE);
            Log.d(TAG, "Contacts table created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create contacts table", e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // For simplicity, just drop and recreate the table
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    /**
     * Save a contact to the database
     * 
     * @param contact the contact to save
     * @return the id of the saved contact
     */
    public long saveContact(Contact contact) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Convert contact fields to JSON
            JSONObject jsonObject = new JSONObject();
            for (String key : contact.getFieldKeys()) {
                jsonObject.put(key, contact.getField(key));
            }

            String jsonData = jsonObject.toString();
            values.put("json_data", jsonData);

            // Create a search index from all values
            StringBuilder searchIndex = new StringBuilder();
            for (String key : contact.getFieldKeys()) {
                searchIndex.append(contact.getField(key)).append(" ");
            }
            values.put("search_index", searchIndex.toString().toLowerCase());

            // Insert or update
            if (contact.getId() > 0) {
                db.update("contacts", values, "_id=?",
                        new String[] { String.valueOf(contact.getId()) });
                return contact.getId();
            } else {
                return db.insert("contacts", null, values);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error saving contact", e);
            return -1;
        }
    }

    /**
     * Save multiple contacts in a batch
     * 
     * @param contacts list of contacts to save
     * @return number of contacts saved
     */
    public int saveContacts(List<Contact> contacts) {
        SQLiteDatabase db = getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            for (Contact contact : contacts) {
                if (saveContact(contact) != -1) {
                    count++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    /**
     * Get all contacts
     * 
     * @return list of all contacts
     */
    public List<Contact> getAllContacts() {
        return getContacts(null, null);
    }

    /**
     * Search contacts
     * 
     * @param query search query
     * @return list of matching contacts
     */
    public List<Contact> searchContacts(String query) {
        if (query == null || query.isEmpty()) {
            return getAllContacts();
        }

        return getContacts("search_index LIKE ?",
                new String[] { "%" + query.toLowerCase() + "%" });
    }

    /**
     * Get contacts with filter
     * 
     * @param selection     SQL selection string
     * @param selectionArgs selection arguments
     * @return list of matching contacts
     */
    private List<Contact> getContacts(String selection, String[] selectionArgs) {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                "contacts",
                new String[] { "_id", "json_data" },
                selection,
                selectionArgs,
                null,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    String jsonData = cursor.getString(cursor.getColumnIndexOrThrow("json_data"));

                    Contact contact = new Contact();
                    contact.setId(id);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        Iterator<String> keys = jsonObject.keys();

                        while (keys.hasNext()) {
                            String key = keys.next();
                            contact.setField(key, jsonObject.optString(key, ""));
                        }

                        contacts.add(contact);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing contact JSON", e);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading contacts", e);
        }

        return contacts;
    }

    /**
     * Delete a contact
     * 
     * @param id contact id
     * @return true if successful
     */
    public boolean deleteContact(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("contacts", "_id=?",
                new String[] { String.valueOf(id) }) > 0;
    }

    /**
     * Delete all contacts
     * 
     * @return number of contacts deleted
     */
    public int deleteAllContacts() {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("contacts", null, null);
    }
}
