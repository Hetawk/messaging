package com.andgroupco.messaging.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessageDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "messages.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "MessageDbHelper";

    // Table creation SQL statement
    private static final String CREATE_MESSAGES_TABLE = "CREATE TABLE messages (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "content TEXT NOT NULL, " +
            "recipient TEXT NOT NULL, " +
            "recipient_type TEXT NOT NULL, " +
            "sent_date INTEGER NOT NULL, " +
            "status TEXT NOT NULL, " +
            "send_count INTEGER DEFAULT 0)";

    public MessageDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables");
        try {
            db.execSQL(CREATE_MESSAGES_TABLE);
            Log.d(TAG, "Database tables created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create database tables", e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        try {
            // Add upgrade paths for each version
            switch (oldVersion) {
                case 1:
                    // Upgrade from version 1 to 2
                    // upgradeToVersion2(db);
                    // fall through
                case 2:
                    // Upgrade from version 2 to 3
                    // upgradeToVersion3(db);
                    // fall through
                default:
                    // For major changes, recreate the table
                    db.execSQL("DROP TABLE IF EXISTS messages");
                    onCreate(db);
                    break;
            }
            Log.i(TAG, "Database upgrade completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Database upgrade failed", e);
            throw e;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle downgrade if needed
        onUpgrade(db, oldVersion, newVersion);
    }

    // Example upgrade method for future use
    private void upgradeToVersion2(SQLiteDatabase db) {
        // Example: Add a new column
        // db.execSQL("ALTER TABLE messages ADD COLUMN priority INTEGER DEFAULT 0");
    }
}
