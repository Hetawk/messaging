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
        try {
            if (message == null) {
                Log.e(TAG, "Cannot send null message");
                if (callback != null) {
                    callback.onFailure("Message is empty");
                }
                return;
            }

            // Default values for empty fields
            if (message.getContent() == null)
                message.setContent("");
            if (message.getRecipient() == null)
                message.setRecipient("");
            if (message.getRecipientType() == null)
                message.setRecipientType("SMS");

            if (message.getSentDate() == null) {
                message.setSentDate(new Date());
            }

            // Initialize send count if not set
            if (message.getSendCount() <= 0) {
                message.setSendCount(1);
            }

            if (hasRecentlySent(message.getRecipient())) {
                showResendDialog(message, callback);
            } else {
                performSend(message, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage", e);
            if (callback != null) {
                callback.onFailure("Message sending failed: " + e.getMessage());
            }
        }
    }

    private boolean hasRecentlySent(String recipient) {
        // Check if message was sent to this recipient recently
        // Return false for now to avoid blocking sends
        return false;
    }

    private void showResendDialog(Message message, SendCallback callback) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle("Resend Message")
                    .setMessage("This message was already sent. Do you want to send it again?")
                    .setPositiveButton("Yes", (dialog, which) -> performSend(message, callback))
                    .setNegativeButton("No", (dialog, which) -> {
                        if (callback != null) {
                            callback.onFailure("Sending cancelled by user");
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing resend dialog", e);
            if (callback != null) {
                callback.onFailure("Could not confirm resend: " + e.getMessage());
            }
        }
    }

    private void performSend(Message message, SendCallback callback) {
        try {
            // Default to SMS if type is missing
            String recipientType = message.getRecipientType();
            if (recipientType == null || recipientType.isEmpty()) {
                recipientType = "SMS";
            }

            switch (recipientType) {
                case "EMAIL":
                    sendEmail(message, callback);
                    break;
                case "SMS":
                default:
                    sendSMS(message, callback);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in performSend", e);
            message.setStatus(STATUS_FAILED);
            try {
                saveMessageToDb(message);
            } catch (Exception dbEx) {
                Log.e(TAG, "Error saving failed message to database", dbEx);
            }

            if (callback != null) {
                callback.onFailure("Error sending message: " + e.getMessage());
            }
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
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            cursor = db.query(
                    "messages",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "sent_date DESC");

            while (cursor != null && cursor.moveToNext()) {
                try {
                    Message message = new Message();

                    int idColumnIndex = cursor.getColumnIndex("_id");
                    if (idColumnIndex != -1) {
                        message.setId(cursor.getLong(idColumnIndex));
                    }

                    int contentColumnIndex = cursor.getColumnIndex("content");
                    if (contentColumnIndex != -1) {
                        message.setContent(cursor.getString(contentColumnIndex));
                    }

                    int recipientColumnIndex = cursor.getColumnIndex("recipient");
                    if (recipientColumnIndex != -1) {
                        message.setRecipient(cursor.getString(recipientColumnIndex));
                    }

                    int typeColumnIndex = cursor.getColumnIndex("recipient_type");
                    if (typeColumnIndex != -1) {
                        message.setRecipientType(cursor.getString(typeColumnIndex));
                    }

                    int statusColumnIndex = cursor.getColumnIndex("status");
                    if (statusColumnIndex != -1) {
                        message.setStatus(cursor.getString(statusColumnIndex));
                    }

                    int countColumnIndex = cursor.getColumnIndex("send_count");
                    if (countColumnIndex != -1) {
                        message.setSendCount(cursor.getInt(countColumnIndex));
                    }

                    int dateColumnIndex = cursor.getColumnIndex("sent_date");
                    if (dateColumnIndex != -1) {
                        long dateLong = cursor.getLong(dateColumnIndex);
                        message.setSentDate(new Date(dateLong));
                    } else {
                        message.setSentDate(new Date());
                    }

                    messages.add(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message from cursor", e);
                    // Continue to next message
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying messages", e);
        } finally {
            // Close cursor safely
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor", e);
                }
            }
        }

        return messages;
    }

    private void saveMessageToDb(Message message) {
        SQLiteDatabase db = null;

        try {
            Log.d(TAG, "Saving message to database: " + message.getId());
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            if (message.getContent() != null) {
                values.put("content", message.getContent());
            } else {
                values.put("content", "");
            }

            if (message.getRecipient() != null) {
                values.put("recipient", message.getRecipient());
            } else {
                values.put("recipient", "");
            }

            if (message.getRecipientType() != null) {
                values.put("recipient_type", message.getRecipientType());
            } else {
                values.put("recipient_type", "SMS");
            }

            if (message.getSentDate() != null) {
                values.put("sent_date", message.getSentDate().getTime());
            } else {
                values.put("sent_date", System.currentTimeMillis());
            }

            if (message.getStatus() != null) {
                values.put("status", message.getStatus());
            } else {
                values.put("status", STATUS_PENDING);
            }

            values.put("send_count", message.getSendCount());

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
