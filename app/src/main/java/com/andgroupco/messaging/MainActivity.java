package com.andgroupco.messaging;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.andgroupco.messaging.services.MessageService;
import com.andgroupco.messaging.adapters.MessageListAdapter;
import com.andgroupco.messaging.utils.PermissionManager;

import java.util.ArrayList;
import java.util.List;
import com.andgroupco.messaging.models.Message;
import com.andgroupco.messaging.base.BaseActivity;
import java.io.InputStream;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private MessageService messageService;
    private MessageListAdapter adapter;
    private RecyclerView messagesList;
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private boolean permissionsGranted = false;
    private String lastOperation = null; // Add this field to track last operation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize components
        messageService = new MessageService(this);
        setupUI();
        setupMessagesList();
        setupLaunchers();

        // Check permissions last
        checkAndRequestPermissions();
    }

    private void setupUI() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup button click listeners
        findViewById(R.id.btnImportContacts).setOnClickListener(v -> showImportOptions());
        findViewById(R.id.btnComposeMessage).setOnClickListener(v -> {
            startActivity(new Intent(this, ComposeMessageActivity.class));
        });

        findViewById(R.id.fabSend).setOnClickListener(v -> {
            Message message = new Message();
            messageService.sendMessage(message, new MessageService.SendCallback() {
                @Override
                public void onSuccess() {
                    updateMessagesList();
                }

                @Override
                public void onFailure(String error) {
                    showError(error);
                }
            });
        });
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions");
        if (PermissionManager.checkAndRequestPermissions(this)) {
            Log.d(TAG, "All permissions granted, setting up features");
            permissionsGranted = true;
            setupFeatures();
        } else {
            Log.d(TAG, "Permissions need to be requested");
        }
    }

    private void setupFeatures() {
        messageService = new MessageService(this);
        setupMessagesList();
        setupLaunchers();
    }

    private void setupMessagesList() {
        if (messagesList == null) {
            messagesList = findViewById(R.id.messagesList);
        }
        if (adapter == null) {
            adapter = new MessageListAdapter(this);
            messagesList.setAdapter(adapter);
            messagesList.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupLaunchers() {
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleContactResult(result.getData());
                    }
                });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleFileResult(result.getData());
                    }
                });
    }

    private void showImportOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Import Contacts")
                .setItems(new String[] { "From Contacts", "From File" }, (dialog, which) -> {
                    if (which == 0) {
                        lastOperation = "CONTACTS";
                        pickContact();
                    } else {
                        lastOperation = "STORAGE";
                        pickFile();
                    }
                })
                .show();
    }

    private void pickContact() {
        if (!PermissionManager.checkPermissionForOperation(this, "CONTACTS")) {
            Log.d(TAG, "Requesting contacts permission");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void pickFile() {
        if (!PermissionManager.checkPermissionForOperation(this, "STORAGE")) {
            Log.d(TAG, "Requesting storage permission");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handleContactResult(Intent data) {
        Log.d(TAG, "Processing contact import");
        try {
            Uri contactUri = data.getData();
            assert contactUri != null;
            try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        String id = cursor.getString(
                                cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                        // Get phone number
                        try (Cursor phoneCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[] { id },
                                null)) {
                            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                String phoneNumber = phoneCursor.getString(
                                        phoneCursor.getColumnIndexOrThrow(
                                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                                Log.d(TAG, "Found phone number: " + phoneNumber);
                                // Save phone number to database or handle as needed
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "Phone number column not found", e);
                        }

                        // Get email
                        try (Cursor emailCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[] { id },
                                null)) {
                            if (emailCursor != null && emailCursor.moveToFirst()) {
                                String email = emailCursor.getString(
                                        emailCursor.getColumnIndexOrThrow(
                                                ContactsContract.CommonDataKinds.Email.ADDRESS));
                                Log.d(TAG, "Found email: " + email);
                                // Save email to database or handle as needed
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "Email column not found", e);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Contact ID column not found", e);
                        throw new Exception("Could not read contact information");
                    }
                }
            }
            updateMessagesList();
            Log.i(TAG, "Contact imported successfully");
        } catch (Exception e) {
            Log.e(TAG, "Contact import failed: " + e.getMessage(), e);
            showError("Error importing contact: " + e.getMessage());
        }
    }

    private void handleFileResult(Intent data) {
        Log.d(TAG, "Processing file import");
        try {
            Uri fileUri = data.getData();
            assert fileUri != null;
            String mimeType = getContentResolver().getType(fileUri);

            if (mimeType != null && (mimeType.equals("text/csv") ||
                    mimeType.equals("application/vnd.ms-excel") ||
                    mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {

                // Read file content
                try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                    // Process CSV/Excel file
                    importContactsFromStream(inputStream, mimeType);
                }
                Log.i(TAG, "File type validated: " + mimeType);
            } else {
                showError("Unsupported file type. Please use CSV or Excel files.");
            }
        } catch (Exception e) {
            Log.e(TAG, "File import failed: " + e.getMessage(), e);
            showError("Error importing file: " + e.getMessage());
        }
    }

    private void importContactsFromStream(InputStream inputStream, String mimeType) {
        // TODO: Implement CSV/Excel parsing
        // For CSV: Use OpenCSV or similar library
        // For Excel: Use Apache POI or similar library
    }

    private void updateMessagesList() {
        try {
            if (messageService == null) {
                messageService = new MessageService(this);
            }
            if (adapter == null) {
                setupMessagesList();
            }
            List<Message> messages = messageService.getAllMessages();
            adapter.updateMessages(messages != null ? messages : new ArrayList<>());
        } catch (Exception e) {
            Log.e(TAG, "Error updating messages", e);
            showError("Error updating messages: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMessagesList();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Permission result received: " + requestCode);

        if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
            Log.d(TAG, "Permissions granted, retrying operation");
            // Retry the last operation
            retryLastOperation();
        } else {
            Log.w(TAG, "Permissions not granted, showing explanation");
            showPermissionExplanationDialog();
        }
    }

    private void retryLastOperation() {
        // Add this method to retry the last attempted operation
        if (lastOperation != null) {
            switch (lastOperation) {
                case "CONTACTS":
                    pickContact();
                    break;
                case "STORAGE":
                    pickFile();
                    break;
                case "SMS":
                    // Handle SMS operation
                    break;
            }
            lastOperation = null;
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires certain permissions to work properly. " +
                        "Would you like to grant them now?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}