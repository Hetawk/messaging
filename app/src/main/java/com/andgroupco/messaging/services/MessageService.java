package com.andgroupco.messaging.services;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import com.andgroupco.messaging.models.Message;
import com.andgroupco.messaging.db.MessageDbHelper;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import android.util.Log;

public class MessageService {
    private static final String TAG = "MessageService";

    // Add status constants
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";

    private final MessageDbHelper dbHelper;
    private final Context context;

    public MessageService(Context context) {
        this.context = context;
        this.dbHelper = new MessageDbHelper(context);
    }

    public void sendMessage(Message message, SendCallback callback) {
        if (hasRecentlySent(message.getRecipient())) {
            showResendDialog(message, callback);
        } else {
            performSend(message, callback);
        }
    }

    private boolean hasRecentlySent(String recipient) {
        // Check if message was sent to this recipient recently
        // Query the database
        return false;
    }

    private void showResendDialog(Message message, SendCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Resend Message")
                .setMessage("This message was already sent. Do you want to send it again?")
                .setPositiveButton("Yes", (dialog, which) -> performSend(message, callback))
                .setNegativeButton("No", null)
                .show();
    }

    private void performSend(Message message, SendCallback callback) {
        switch (message.getRecipientType()) {
            case "EMAIL":
                sendEmail(message, callback);
                break;
            case "SMS":
                sendSMS(message, callback);
                break;
        }
    }

    private void sendEmail(Message message, SendCallback callback) {
        Log.i(TAG, "Attempting to send email to: " + message.getRecipient());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, message.getRecipient().split(","));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Inspirational Message");
        intent.putExtra(Intent.EXTRA_TEXT, message.getContent());

        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."));
            message.setStatus(STATUS_SENT);
            saveMessageToDb(message);
            callback.onSuccess();
            Log.d(TAG, "Email sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Email send failed: " + e.getMessage(), e);
            message.setStatus(STATUS_FAILED);
            saveMessageToDb(message);
            callback.onFailure("Failed to send email: " + e.getMessage());
        }
    }

    private void sendSMS(Message message, SendCallback callback) {
        Log.i(TAG, "Attempting to send SMS to: " + message.getRecipient());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("smsto:" + message.getRecipient()));
        intent.putExtra("sms_body", message.getContent());

        try {
            context.startActivity(intent);
            message.setStatus(STATUS_SENT);
            saveMessageToDb(message);
            callback.onSuccess();
            Log.d(TAG, "SMS sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "SMS send failed: " + e.getMessage(), e);
            message.setStatus(STATUS_FAILED);
            saveMessageToDb(message);
            callback.onFailure("Failed to send SMS: " + e.getMessage());
        }
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "messages",
                null,
                null,
                null,
                null,
                null,
                "sent_date DESC")) {

            while (cursor.moveToNext()) {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
                message.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                message.setRecipient(cursor.getString(cursor.getColumnIndexOrThrow("recipient")));
                message.setRecipientType(cursor.getString(cursor.getColumnIndexOrThrow("recipient_type")));
                message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                message.setSendCount(cursor.getInt(cursor.getColumnIndexOrThrow("send_count")));

                long dateLong = cursor.getLong(cursor.getColumnIndexOrThrow("sent_date"));
                message.setSentDate(new Date(dateLong));

                messages.add(message);
            }
        }

        return messages;
    }

    private void saveMessageToDb(Message message) {
        Log.d(TAG, "Saving message to database: " + message.getId());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("content", message.getContent());
        values.put("recipient", message.getRecipient());
        values.put("recipient_type", message.getRecipientType());
        values.put("sent_date", message.getSentDate().getTime());
        values.put("status", message.getStatus());
        values.put("send_count", message.getSendCount());

        try {
            db.insert("messages", null, values);
            Log.d(TAG, "Message saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save message: " + e.getMessage(), e);
            throw e;
        }
    }

    public interface SendCallback {
        void onSuccess();

        void onFailure(String error);
    }
}
