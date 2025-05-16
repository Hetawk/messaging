package com.andgroupco.messaging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Keep if used for other app data
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment; // Keep this import
import android.widget.FrameLayout; // Add this import
import android.view.ViewTreeObserver; // Add this import

import com.andgroupco.messaging.fragments.ContactsFragment;
import com.andgroupco.messaging.fragments.HistoryFragment;
import com.andgroupco.messaging.fragments.HomeFragment;
import com.andgroupco.messaging.fragments.MessagesFragment;
import com.andgroupco.messaging.services.MessageService;
import com.andgroupco.messaging.utils.PermissionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import com.andgroupco.messaging.base.BaseActivity;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    // If PREFS_NAME was only for crash data, it can be removed or repurposed.
    // For now, let's assume it might be used for other app settings.
    private static final String PREFS_NAME = "app_prefs"; // Renamed if it was crash_prefs

    private MessageService messageService;
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private boolean permissionsGranted = false;
    private String lastOperation = null; // Add this field to track last operation
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Removed: Uncaught exception handler
        // Removed: Safe mode checks

        try {
            // Standard onCreate logic
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            // Initialize UI components first (important for preventing NPEs)
            try {
                setupUI();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up UI", e);
                showError("Error initializing UI: " + e.getMessage());
            }

            // Setup other components - but don't rely on permissions yet
            try {
                setupNavigationAndFragments();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up navigation", e);
                showError("Error initializing navigation: " + e.getMessage());
            }

            try {
                setupLaunchers();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up launchers", e);
            }

            // Check permissions last - after all UI is initialized
            try {
                checkAndRequestPermissions();
            } catch (Exception e) {
                Log.e(TAG, "Error checking permissions", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            try {
                // Attempt to set a very basic error view if main layout fails
                // This part might need a dedicated error layout if R.layout.activity_main
                // itself is problematic
                setContentView(R.layout.activity_main); // Or a simpler error layout
                showError("Application failed to start properly: " + e.getMessage());
            } catch (Exception e2) {
                Log.e(TAG, "Could not show error message or set content view", e2);
                // Fallback to a Toast if AlertDialog fails
                Toast.makeText(this, "Critical application error. Please restart.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showSafeModeMessage() { // This method might be repurposed or removed if safe mode is gone
        // Create a simple view in case the main layout fails
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Application Issue") // Changed title
                    .setMessage(
                            "The app encountered an issue. Some features may be limited. Would you like to reset app data?") // General
                                                                                                                             // message
                    .setPositiveButton("Reset App Data", (dialog, which) -> {
                        clearAppData();
                        restartApp(); // Restart normally, without safe mode flag
                    })
                    .setNegativeButton("Continue", null)
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show issue dialog", e);
            Toast.makeText(this, "Application encountered an issue.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void clearAppData() {
        try {
            // Clear preferences, databases, etc.
            // If PREFS_NAME was "crash_prefs" and only for crashes, this might change.
            // Assuming PREFS_NAME is now "app_prefs" for general app settings.
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "App data cleared.");
            // Add more data clearing as needed (e.g., databases)
        } catch (Exception e) {
            Log.e(TAG, "Error clearing app data", e);
        }
    }

    private void restartApp() {
        try {
            Log.d(TAG, "Restarting app.");
            Intent intent = new Intent(this, MainActivity.class);
            // Removed: intent.putExtra("SAFE_MODE", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart app", e);
        }
    }

    private void setupUI() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Adjust fragment container padding for BottomNavigationView
        if (bottomNavigation != null) {
            final FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                bottomNavigation.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                // Remove the listener to prevent multiple calls
                                bottomNavigation.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                // Get the height of the BottomNavigationView
                                int bottomNavHeight = bottomNavigation.getHeight();

                                // Apply this height as bottom padding to the fragment container
                                // Also preserve existing padding
                                fragmentContainer.setPadding(
                                        fragmentContainer.getPaddingLeft(),
                                        fragmentContainer.getPaddingTop(),
                                        fragmentContainer.getPaddingRight(),
                                        bottomNavHeight + fragmentContainer.getPaddingBottom() // Add to existing bottom
                                                                                               // padding if any
                                );
                            }
                        });
            }
        }
    }

    private void setupNavigationAndFragments() {
        try {
            if (bottomNavigation != null) {
                bottomNavigation.setOnItemSelectedListener(item -> {
                    Fragment selectedFragment = null;
                    String title = "Messaging";

                    try {
                        int itemId = item.getItemId();
                        if (itemId == R.id.navigation_home) {
                            selectedFragment = HomeFragment.newInstance();
                            title = "Home";
                        } else if (itemId == R.id.navigation_contacts) {
                            selectedFragment = ContactsFragment.newInstance();
                            title = "Contacts";
                        } else if (itemId == R.id.navigation_messages) {
                            selectedFragment = MessagesFragment.newInstance();
                            title = "Messages";
                        } else if (itemId == R.id.navigation_history) {
                            selectedFragment = HistoryFragment.newInstance();
                            title = "History";
                        }

                        if (selectedFragment != null && !isFinishing()) {
                            try {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, selectedFragment)
                                        .commitAllowingStateLoss(); // Use commitAllowingStateLoss

                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(title);
                                }
                                return true;
                            } catch (Exception e) {
                                Log.e(TAG, "Error replacing fragment", e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in navigation selection", e);
                    }
                    return false;
                });

                // Set default selection - wrapped in try/catch for safety
                try {
                    bottomNavigation.setSelectedItemId(R.id.navigation_home);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set default navigation", e);
                    // Fallback - try to load home fragment directly
                    try {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, HomeFragment.newInstance())
                                .commitAllowingStateLoss();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Home");
                        }
                    } catch (Exception e2) {
                        Log.e(TAG, "Failed to load fallback fragment", e2);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in setupNavigationAndFragments", e);
            // Show user-friendly error
            try {
                showError("Application initialization failed. Please restart the app.");
            } catch (Exception errorEx) {
                // Last resort if even showing error fails
                Log.e(TAG, "Couldn't show error message", errorEx);
            }
        }
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions");
        try {
            if (PermissionManager.checkAndRequestPermissions(this)) {
                Log.d(TAG, "All permissions granted, setting up features");
                permissionsGranted = true;
                setupFeatures();
            } else {
                Log.d(TAG, "Permissions need to be requested");
            }

            // Check notification permission separately - this will only do something on
            // Android 13+
            PermissionManager.checkPermissionForOperation(this, "NOTIFICATIONS");

        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
            // Continue with limited functionality
            setupFeatures();
        }
    }

    private void setupFeatures() {
        try {
            messageService = new MessageService(this);
            // Instead of calling updateMessagesList(), refresh any visible fragments
            refreshCurrentFragment();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up features", e);
        }
    }

    // Add this method to refresh the current fragment
    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof HistoryFragment) {
            ((HistoryFragment) currentFragment).refreshMessages();
        } else if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).refreshData();
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

    public void showImportOptions() {
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
            // Refresh fragments after import instead of calling updateMessagesList()
            refreshCurrentFragment();
            showSuccess("Contact imported successfully");

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

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Check if permissions are granted before refreshing
            if (permissionsGranted) {
                refreshCurrentFragment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Permission result received: " + requestCode);

        try {
            if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
                Log.d(TAG, "Permissions granted, retrying operation");
                // Initialize services that require permissions
                setupFeatures();
                // Retry the last operation
                retryLastOperation();
            } else {
                Log.w(TAG, "Permissions not granted, showing explanation");
                showPermissionExplanationDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling permission results", e);
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            // Add any state saving here
        } catch (Exception e) {
            Log.e(TAG, "Error saving instance state", e);
        }
    }

    // Add robust crash handlers
    @Override
    protected void attachBaseContext(Context newBase) {
        try {
            super.attachBaseContext(newBase);
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
        }
    }
}