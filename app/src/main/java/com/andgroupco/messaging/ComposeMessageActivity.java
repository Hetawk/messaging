package com.andgroupco.messaging;

import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.andgroupco.messaging.base.BaseActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.andgroupco.messaging.services.MessageService;
import com.andgroupco.messaging.models.Message;
import com.andgroupco.messaging.utils.PermissionManager;

import java.util.Date;

public class ComposeMessageActivity extends BaseActivity {
    private static final String TAG = "ComposeMessage";
    private RadioGroup sendMethodGroup;
    private TextInputEditText recipientsInput;
    private TextInputEditText messageInput;
    private MessageService messageService;
    private String lastOperation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        setSupportActionBar(findViewById(R.id.composeToolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        messageService = new MessageService(this);
        initializeViews();
    }

    private void initializeViews() {
        sendMethodGroup = findViewById(R.id.sendMethodGroup);
        recipientsInput = findViewById(R.id.recipientsInput);
        messageInput = findViewById(R.id.messageInput);

        findViewById(R.id.sendButton).setOnClickListener(v -> validateAndSend());
    }

    private void validateAndSend() {
        Log.d(TAG, "Validating message before sending");
        String sendMethod = sendMethodGroup.getCheckedRadioButtonId() == R.id.radioEmail ? "EMAIL" : "SMS";

        // Check permissions based on send method
        if (sendMethod.equals("SMS") && !PermissionManager.checkPermissionForOperation(this, "SMS")) {
            Log.d(TAG, "Requesting SMS permission");
            lastOperation = "SEND";
            return;
        }

        // Continue with sending
        try {
            String recipients = recipientsInput.getText().toString();
            String message = messageInput.getText().toString();

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

        if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
            if (lastOperation != null && lastOperation.equals("SEND")) {
                validateAndSend();
            }
        } else {
            showError("Required permissions not granted");
        }
        lastOperation = null;
    }
}
