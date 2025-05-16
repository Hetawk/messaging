package com.andgroupco.messaging;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.andgroupco.messaging.base.BaseActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Import TextInputLayout
import com.andgroupco.messaging.services.MessageService;
import com.andgroupco.messaging.models.Message;
import com.andgroupco.messaging.utils.PermissionManager;

import java.util.Date;

public class ComposeMessageActivity extends BaseActivity {
    private static final String TAG = "ComposeMessage";
    private RadioGroup sendMethodGroup;
    private TextInputLayout recipientsInputLayout; // Changed to TextInputLayout
    private TextInputEditText recipientsInput;
    private TextInputLayout messageInputLayout; // Changed to TextInputLayout
    private TextInputEditText messageInput;
    private Button btnSelectImportedContacts; // Placeholder button
    private MessageService messageService;
    private String lastOperation = null;

    private ActivityResultLauncher<Intent> contactPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        try {
            Toolbar toolbar = findViewById(R.id.composeToolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }

            messageService = new MessageService(this);
            initializeViews();
            setupContactPicker();

        } catch (Exception e) {
            Log.e(TAG, "Error during initialization", e);
            showError("Error initializing: " + e.getMessage());
        }
    }

    private void initializeViews() {
        try {
            sendMethodGroup = findViewById(R.id.sendMethodGroup);
            recipientsInputLayout = findViewById(R.id.recipientsInputLayout);
            recipientsInput = findViewById(R.id.recipientsInput);
            messageInputLayout = findViewById(R.id.messageInputLayout);
            messageInput = findViewById(R.id.messageInput);
            btnSelectImportedContacts = findViewById(R.id.btnSelectImportedContacts);

            if (sendMethodGroup != null && sendMethodGroup.getCheckedRadioButtonId() == -1) {
                if (findViewById(R.id.radioEmail) != null) { // Default to Email
                    sendMethodGroup.check(R.id.radioEmail);
                }
            }
            updateRecipientInputType(); // Set initial input type

            sendMethodGroup.setOnCheckedChangeListener((group, checkedId) -> updateRecipientInputType());

            if (recipientsInputLayout != null) {
                recipientsInputLayout.setEndIconOnClickListener(v -> {
                    if (PermissionManager.checkPermissionForOperation(this, "CONTACTS")) {
                        pickContact();
                    } else {
                        lastOperation = "PICK_CONTACT";
                        // PermissionManager.checkPermissionForOperation will request it.
                    }
                });
            }

            // Placeholder for imported contacts button
            if (btnSelectImportedContacts != null) {
                btnSelectImportedContacts.setOnClickListener(v -> {
                    // TODO: Implement UI to select from locally stored "imported" contacts
                    Toast.makeText(this, "Selecting from imported contacts - Not yet implemented", Toast.LENGTH_SHORT)
                            .show();
                });
            }

            if (findViewById(R.id.sendButton) != null) {
                findViewById(R.id.sendButton).setOnClickListener(v -> validateAndSend());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            showError("Error setting up the form: " + e.getMessage());
        }
    }

    private void updateRecipientInputType() {
        if (sendMethodGroup == null || recipientsInput == null)
            return;
        int checkedId = sendMethodGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioEmail) {
            recipientsInput.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            recipientsInputLayout.setHint("Recipients (e.g., email@example.com)");
        } else if (checkedId == R.id.radioSms) {
            recipientsInput.setInputType(
                    android.text.InputType.TYPE_CLASS_PHONE | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            recipientsInputLayout.setHint("Recipients (e.g., 1234567890)");
        }
    }

    private void setupContactPicker() {
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            String contactInfo = getContactInfo(contactUri);
                            if (contactInfo != null && !contactInfo.isEmpty()) {
                                String currentRecipients = recipientsInput.getText() != null
                                        ? recipientsInput.getText().toString()
                                        : "";
                                if (currentRecipients.isEmpty()) {
                                    recipientsInput.setText(contactInfo);
                                } else {
                                    // Append with a comma and space, or just the info if it's the first one
                                    recipientsInput.append(
                                            (currentRecipients.endsWith(", ") || currentRecipients.endsWith(","))
                                                    ? contactInfo
                                                    : ", " + contactInfo);
                                }
                            }
                        }
                    }
                });
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK);
        int checkedId = sendMethodGroup.getCheckedRadioButtonId();

        if (checkedId == R.id.radioEmail) {
            pickContactIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
        } else if (checkedId == R.id.radioSms) {
            pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        } else {
            Toast.makeText(this, "Please select send method first", Toast.LENGTH_SHORT).show();
            return;
        }
        contactPickerLauncher.launch(pickContactIntent);
    }

    private String getContactInfo(Uri contactUri) {
        String info = null;
        Cursor cursor = null;
        try {
            String[] projection;
            String column;

            int checkedId = sendMethodGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radioEmail) {
                projection = new String[] { ContactsContract.CommonDataKinds.Email.ADDRESS };
                column = ContactsContract.CommonDataKinds.Email.ADDRESS;
            } else if (checkedId == R.id.radioSms) {
                projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER };
                column = ContactsContract.CommonDataKinds.Phone.NUMBER;
            } else {
                return null;
            }

            cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(column);
                if (columnIndex >= 0) {
                    info = cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get contact info", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return info;
    }

    private void validateAndSend() {
        Log.d(TAG, "Validating message before sending");
        try {
            if (sendMethodGroup == null || recipientsInput == null || messageInput == null) {
                showError("UI components not initialized properly");
                return;
            }

            String sendMethod = sendMethodGroup.getCheckedRadioButtonId() == R.id.radioEmail ? "EMAIL" : "SMS";

            // Check permissions based on send method
            if (sendMethod.equals("SMS") && !PermissionManager.checkPermissionForOperation(this, "SMS")) {
                Log.d(TAG, "Requesting SMS permission");
                lastOperation = "SEND";
                return;
            }

            // Get input values with null checks
            String recipients = recipientsInput.getText() != null ? recipientsInput.getText().toString() : "";
            String message = messageInput.getText() != null ? messageInput.getText().toString() : "";

            if (recipients.isEmpty() || message.isEmpty()) {
                showError("Please fill in all fields");
                return;
            }

            Message messageObj = new Message();
            messageObj.setContent(message);
            messageObj.setRecipient(recipients);
            messageObj.setRecipientType(sendMethod);
            messageObj.setStatus("PENDING");
            messageObj.setSentDate(new Date());

            if (messageService == null) {
                messageService = new MessageService(this);
            }

            Log.i(TAG, "Message validated, attempting to send");
            messageService.sendMessage(messageObj, new MessageService.SendCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Message sent successfully");
                    showSuccess("Message sent successfully");
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Message send failed: " + error);
                    showError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing message: " + e.getMessage(), e);
            showError("Error preparing message: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
                if ("PICK_CONTACT".equals(lastOperation)) {
                    pickContact();
                } else if ("SEND".equals(lastOperation)) {
                    validateAndSend();
                }
            } else {
                showError("Required permissions not granted");
            }
            lastOperation = null;
        } catch (Exception e) {
            Log.e(TAG, "Error handling permission result", e);
            showError("Error with permissions: " + e.getMessage());
        }
    }
}
