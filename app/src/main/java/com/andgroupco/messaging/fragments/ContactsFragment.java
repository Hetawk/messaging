package com.andgroupco.messaging.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andgroupco.messaging.R;
import com.andgroupco.messaging.adapters.ContactListAdapter;
import com.andgroupco.messaging.db.ContactDbHelper;
import com.andgroupco.messaging.models.Contact;
import com.andgroupco.messaging.utils.ContactImportHelper;
import com.andgroupco.messaging.utils.FileParserUtils;
import com.andgroupco.messaging.utils.PermissionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment implements ContactListAdapter.ContactSelectionListener,
        ContactListAdapter.ContactActionListener {
    private static final String TAG = "ContactsFragment";

    private RecyclerView contactsRecyclerView;
    private TextInputEditText searchInput;
    private ContactListAdapter adapter;
    private ContactDbHelper dbHelper;
    private TextView emptyView;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    private List<Contact> allContacts = new ArrayList<>();
    private static final int IMPORT_OPTION_PHONE = 1;
    private static final int IMPORT_OPTION_FILE = 2;
    private static final int IMPORT_OPTION_ONEDRIVE = 3;
    private static final int IMPORT_OPTION_GOOGLE = 4;
    private static final int IMPORT_OPTION_SAMPLE = 5;
    private TextView contactCountView;
    private ActionBar actionBar;
    private MenuItem selectAllMenuItem;
    private MenuItem deleteMenuItem;

    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize database helper - with null check protection
            if (getContext() != null) {
                dbHelper = new ContactDbHelper(requireContext());
            } else {
                Log.e(TAG, "Context is null, cannot initialize database");
                return; // Exit early if context is null
            }

            // Initialize views with null checks
            contactsRecyclerView = view.findViewById(R.id.contactsList);
            searchInput = view.findViewById(R.id.searchContacts);
            emptyView = view.findViewById(R.id.emptyContactsView);
            Button importButton = view.findViewById(R.id.btnImportContacts);
            FloatingActionButton fabAddContact = view.findViewById(R.id.fabAddContact);

            // Check if activity is available and is AppCompatActivity
            if (getActivity() instanceof AppCompatActivity) {
                actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            }

            // Find contactCountView with null check
            contactCountView = view.findViewById(R.id.contactCountView);

            // Setup RecyclerView with null checks
            if (contactsRecyclerView != null && getContext() != null) {
                contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                adapter = new ContactListAdapter(requireContext());
                adapter.setSelectionListener(this);
                adapter.setActionListener(this);
                contactsRecyclerView.setAdapter(adapter);
            }

            // Setup search functionality with null checks
            if (searchInput != null) {
                searchInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        filterContacts(s.toString());
                    }
                });
            }

            // Setup import button with null check
            if (importButton != null) {
                importButton.setOnClickListener(v -> showImportOptionsDialog());
            }

            // Setup FAB with null check
            if (fabAddContact != null) {
                fabAddContact.setOnClickListener(v -> showAddContactDialog());
            }

            // Setup launchers
            setupLaunchers();

            // Load contacts if DB is initialized
            if (dbHelper != null) {
                loadContacts();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up ContactsFragment", e);
            try {
                if (getView() != null && isAdded()) {
                    showError("Error initializing contacts view: " + e.getMessage());
                }
            } catch (Exception e2) {
                Log.e(TAG, "Error showing error message", e2);
            }
        }
    }

    private void setupLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleFileResult(result.getData());
                    }
                });

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleContactResult(result.getData());
                    }
                });
    }

    private void showImportOptionsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_import_options);

        // Set up click listeners for each import option
        CardView importFromPhone = dialog.findViewById(R.id.importFromPhone);
        CardView importFromExcel = dialog.findViewById(R.id.importFromExcel);
        CardView importFromOneDrive = dialog.findViewById(R.id.importFromOneDrive);
        CardView importFromGoogle = dialog.findViewById(R.id.importFromGoogle);
        CardView importFromSample = dialog.findViewById(R.id.importFromSample);

        importFromPhone.setOnClickListener(v -> {
            dialog.dismiss();
            startImportProcess(IMPORT_OPTION_PHONE);
        });

        importFromExcel.setOnClickListener(v -> {
            dialog.dismiss();
            startImportProcess(IMPORT_OPTION_FILE);
        });

        importFromOneDrive.setOnClickListener(v -> {
            dialog.dismiss();
            startImportProcess(IMPORT_OPTION_ONEDRIVE);
        });

        importFromGoogle.setOnClickListener(v -> {
            dialog.dismiss();
            startImportProcess(IMPORT_OPTION_GOOGLE);
        });

        importFromSample.setOnClickListener(v -> {
            dialog.dismiss();
            startImportProcess(IMPORT_OPTION_SAMPLE);
        });

        dialog.show();
    }

    private void startImportProcess(int importOption) {
        switch (importOption) {
            case IMPORT_OPTION_PHONE:
                importFromPhoneContacts();
                break;
            case IMPORT_OPTION_FILE:
                importFromFile();
                break;
            case IMPORT_OPTION_ONEDRIVE:
                importFromOneDrive();
                break;
            case IMPORT_OPTION_GOOGLE:
                importFromGoogle();
                break;
            case IMPORT_OPTION_SAMPLE:
                importFromSampleData();
                break;
        }
    }

    private void importFromPhoneContacts() {
        if (!PermissionManager.checkPermissionForOperation(requireActivity(), "CONTACTS")) {
            Log.d(TAG, "Requesting contacts permission");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE);
        contactPickerLauncher.launch(intent);
    }

    private void importFromFile() {
        // For modern Android, we should use the Storage Access Framework
        // This doesn't actually require storage permissions since the system
        // file picker handles the file access
        Log.d(TAG, "Importing file using Storage Access Framework");

        // For the file picker, we generally don't need explicit storage permissions
        // but we'll check anyway for backward compatibility
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (!PermissionManager.checkPermissionForOperation(requireActivity(), "STORAGE")) {
                Log.d(TAG, "Requesting storage permission for older Android");
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE); // Ensure we get openable files
        String[] mimeTypes = { "text/csv", "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Important for file access

        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching file picker", e);
            showError("Could not open file picker: " + e.getMessage());
        }
    }

    private void importFromOneDrive() {
        try {
            // Check if OneDrive is installed
            final Intent oneDriveIntent = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage("com.microsoft.skydrive");

            // Create alert dialog with options
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("Import from OneDrive")
                    .setItems(new String[] { "Use OneDrive App", "Enter OneDrive Link" }, (dialog, which) -> {
                        if (which == 0) {
                            // Option 1: Use OneDrive app
                            if (oneDriveIntent != null) {
                                // Launch OneDrive with GET_CONTENT action
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("*/*");
                                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                                        new String[] { "text/csv", "application/vnd.ms-excel" });

                                // Try to set OneDrive as the only target
                                intent.setPackage("com.microsoft.skydrive");

                                filePickerLauncher.launch(intent);
                            } else {
                                // OneDrive not installed, prompt to install or use alternative
                                promptInstallOneDrive();
                            }
                        } else {
                            // Option 2: Enter OneDrive link directly
                            showOneDriveLinkInputDialog();
                        }
                    });

            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error launching OneDrive options", e);
            showError("Error accessing OneDrive: " + e.getMessage());
        }
    }

    private void promptInstallOneDrive() {
        new AlertDialog.Builder(requireContext())
                .setTitle("OneDrive Not Found")
                .setMessage("Would you like to install Microsoft OneDrive from the Play Store?")
                .setPositiveButton("Install", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.microsoft.skydrive")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.skydrive")));
                    }
                })
                .setNegativeButton("Enter Link Instead", (dialog, which) -> {
                    showOneDriveLinkInputDialog();
                })
                .setNeutralButton("Choose Different Method", (dialog, which) -> {
                    showImportOptionsDialog();
                })
                .show();
    }

    private void showOneDriveLinkInputDialog() {
        // Create a dialog with an EditText for link input
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("https://onedrive.live.com/...");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        layout.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Enter OneDrive Link")
                .setMessage("Paste a direct download link to your CSV or Excel file")
                .setView(layout)
                .setPositiveButton("Import", (dialog, which) -> {
                    String link = input.getText().toString().trim();
                    if (!link.isEmpty()) {
                        importFromOneDriveLink(link);
                    } else {
                        showError("Please enter a valid link");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void importFromOneDriveLink(String link) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Downloading file...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create async task to download the file
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Convert sharing link to direct download link if needed
                if (link.contains("1drv.ms") || link.contains("onedrive.live.com/share")) {
                    // This is a sharing link, might need conversion
                    // A proper implementation would handle this conversion
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Please use a direct download link");
                    });
                    return;
                }

                URL url = new URL(link);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Store response code in a variable to avoid calling it multiple times
                final int responseCode;
                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Failed to get response from server: " + e.getMessage());
                    });
                    return;
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Failed to download file: HTTP " + responseCode);
                    });
                    return;
                }

                // Get file name and type from URL or headers
                String fileName = link.substring(link.lastIndexOf('/') + 1);
                String initialFileType = "text/csv"; // Default to CSV
                String contentType = connection.getContentType();
                if (contentType != null) {
                    if (contentType.contains("excel") || fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                        initialFileType = "application/vnd.ms-excel";
                    }
                }
                final String fileType = initialFileType; // Create a final copy for the lambda

                // Download the file to a temporary location
                File tempFile = File.createTempFile("onedrive_import", getFileExtension(fileName));
                try (InputStream inputStream = connection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                // Process the downloaded file
                final Uri fileUri = Uri.fromFile(tempFile); // Make fileUri final for the lambda
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    try {
                        List<Contact> importedContacts = FileParserUtils.parseContactsFromUri(
                                requireContext(), fileUri, fileType);

                        if (importedContacts.isEmpty()) {
                            showError("No contacts found in file");
                            return;
                        }

                        // Show import confirmation dialog
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Import Contacts")
                                .setMessage("Found " + importedContacts.size() + " contacts. Import them?")
                                .setPositiveButton("Import", (dialog, which) -> {
                                    // Save contacts to database
                                    int saved = dbHelper.saveContacts(importedContacts);
                                    showSuccess(saved + " contacts imported successfully");

                                    // Refresh contact list
                                    loadContacts();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } catch (Exception e) {
                        showError("Error processing file: " + e.getMessage());
                        Log.e(TAG, "Error processing downloaded file", e);
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Error downloading file: " + e.getMessage());
                });
                Log.e(TAG, "Error downloading file from OneDrive link", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fileName.substring(lastDot);
        }
        return ".tmp";
    }

    private void importFromGoogle() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");

            // Try to set Google Drive as the preferred handler
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            intent.putExtra("android.provider.extra.INITIAL_URI", "content://com.google.android.apps.docs.storage");

            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching Google Drive", e);
            showError("Error accessing Google Drive: " + e.getMessage());

            // Fall back to regular file picker
            importFromFile();
        }
    }

    private void importFromSampleData() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Import Sample Data")
                .setMessage("This will import sample contacts from the bundled database. Proceed?")
                .setPositiveButton("Import", (dialog, which) -> {
                    try {
                        int count = ContactImportHelper.importSampleContacts(requireContext());
                        if (count > 0) {
                            showSuccess(count + " sample contacts imported successfully");
                            loadContacts();
                        } else {
                            showError("No sample contacts were imported");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error importing sample data", e);
                        showError("Error importing sample data: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleContactResult(Intent data) {
        Log.d(TAG, "Processing contact import from phone");
        try {
            Uri contactUri = data.getData();
            if (contactUri == null) {
                showError("No contact selected");
                return;
            }

            // Create a new contact
            Contact contact = new Contact();
            boolean hasData = false;

            // Query for contact name
            try (Cursor cursor = requireContext().getContentResolver().query(
                    contactUri, null, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor
                            .getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        String displayName = cursor.getString(displayNameIndex);
                        contact.setField("Name", displayName);
                        hasData = true;
                        Log.d(TAG, "Found contact name: " + displayName);
                    }

                    int idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID);
                    if (idIndex != -1) {
                        String id = cursor.getString(idIndex);

                        // Query for phone numbers
                        try (Cursor phoneCursor = requireContext().getContentResolver().query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[] { id },
                                null)) {

                            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                int phoneIndex = phoneCursor.getColumnIndex(
                                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                                if (phoneIndex != -1) {
                                    String phoneNumber = phoneCursor.getString(phoneIndex);
                                    contact.setField("Phone", phoneNumber);
                                    hasData = true;
                                    Log.d(TAG, "Found phone number: " + phoneNumber);
                                }
                            }
                        }

                        // Query for email addresses
                        try (Cursor emailCursor = requireContext().getContentResolver().query(
                                android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[] { id },
                                null)) {

                            if (emailCursor != null && emailCursor.moveToFirst()) {
                                int emailIndex = emailCursor.getColumnIndex(
                                        android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS);
                                if (emailIndex != -1) {
                                    String email = emailCursor.getString(emailIndex);
                                    contact.setField("Email", email);
                                    hasData = true;
                                    Log.d(TAG, "Found email: " + email);
                                }
                            }
                        }
                    }
                }
            }

            if (!hasData) {
                showError("No data found in the selected contact");
                return;
            }

            // Save contact to database
            long id = dbHelper.saveContact(contact);
            if (id > 0) {
                showSuccess("Contact imported successfully");
                loadContacts();
            } else {
                showError("Failed to save contact");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error importing contact from phone", e);
            showError("Error importing contact: " + e.getMessage());
        }
    }

    private void handleFileResult(Intent data) {
        if (data == null) {
            Log.e(TAG, "handleFileResult called with null intent");
            return;
        }

        try {
            Uri fileUri = data.getData();
            if (fileUri == null) {
                showError("No file selected");
                return;
            }

            Log.d(TAG, "Processing file: " + fileUri.toString());

            // Check if context is available
            if (getContext() == null) {
                Log.e(TAG, "Context is null in handleFileResult");
                return;
            }

            // Take the persistence permission - needed for SCOPED STORAGE
            try {
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                requireContext().getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                Log.d(TAG, "Took persistable URI permission for: " + fileUri);
            } catch (Exception e) {
                Log.w(TAG, "Could not take persistable permission: " + e.getMessage());
                // Continue anyway - might still work for this session
            }

            String mimeType = requireContext().getContentResolver().getType(fileUri);
            if (mimeType == null) {
                // Try to infer from extension
                String path = fileUri.getPath();
                if (path != null) {
                    if (path.endsWith(".csv")) {
                        mimeType = "text/csv";
                    } else if (path.endsWith(".xls")) {
                        mimeType = "application/vnd.ms-excel";
                    } else if (path.endsWith(".xlsx")) {
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    }
                }
            }

            Log.d(TAG, "File MIME type: " + mimeType);

            if (mimeType == null) {
                showError("Unknown file type");
                return;
            }

            // Read the file content directly
            StringBuilder fileContent = new StringBuilder();
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            }

            if (fileContent.length() == 0) {
                showError("File is empty");
                return;
            }

            Log.d(TAG, "File content preview: " + fileContent.substring(0, Math.min(100, fileContent.length())));

            // Parse contacts from file
            List<Contact> importedContacts = new ArrayList<>();
            String[] lines = fileContent.toString().split("\n");

            if (lines.length < 2) {
                showError("File has insufficient data (needs header + at least one record)");
                return;
            }

            // Parse header line
            List<String> headers = FileParserUtils.parseCsvLine(lines[0]);

            // Parse data lines
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty())
                    continue;

                List<String> values = FileParserUtils.parseCsvLine(line);

                if (values.size() < 2)
                    continue; // Skip lines with insufficient data

                Contact contact = new Contact();
                for (int j = 0; j < Math.min(headers.size(), values.size()); j++) {
                    String header = headers.get(j);
                    String value = values.get(j);
                    if (header != null && !header.isEmpty()) {
                        contact.setField(header, value);
                    }
                }

                importedContacts.add(contact);
            }

            if (importedContacts.isEmpty()) {
                showError("No valid contacts found in file");
                return;
            }

            Log.d(TAG, "Found " + importedContacts.size() + " contacts in file");

            // Show import confirmation dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Import Contacts")
                    .setMessage("Found " + importedContacts.size() + " contacts. Import them?")
                    .setPositiveButton("Import", (dialog, which) -> {
                        // Save contacts to database
                        int saved = dbHelper.saveContacts(importedContacts);
                        showSuccess(saved + " contacts imported successfully");

                        // Refresh contact list
                        loadContacts();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (IOException e) {
            Log.e(TAG, "Error handling file import", e);
            showError("Error importing file: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during import", e);
            showError("Import failed: " + e.getMessage());
        }
    }

    /**
     * Show dialog to add a new contact
     */
    private void showAddContactDialog() {
        // Create dialog view
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_contact, null);

        // Get references to input fields
        TextInputEditText nameInput = dialogView.findViewById(R.id.etName);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.etPhone);
        TextInputEditText emailInput = dialogView.findViewById(R.id.etEmail);

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Contact")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    // Create and save new contact
                    Contact contact = new Contact();
                    contact.setField("Name", nameInput.getText().toString().trim());
                    contact.setField("Phone", phoneInput.getText().toString().trim());
                    contact.setField("Email", emailInput.getText().toString().trim());

                    // Save to database
                    long id = dbHelper.saveContact(contact);
                    if (id > 0) {
                        showSuccess("Contact added successfully");
                        loadContacts();
                    } else {
                        showError("Failed to add contact");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.contacts_menu, menu);
        selectAllMenuItem = menu.findItem(R.id.action_select_all);
        deleteMenuItem = menu.findItem(R.id.action_delete);
        updateMenuItemsVisibility();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_select_all) {
            if (adapter.isInSelectionMode()) {
                adapter.selectAll();
            }
            return true;
        } else if (itemId == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        List<Contact> selectedContacts = adapter.getSelectedContacts();
        if (selectedContacts.isEmpty()) {
            showError("No contacts selected");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Contacts")
                .setMessage("Are you sure you want to delete " + selectedContacts.size() + " contacts?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteSelectedContacts(selectedContacts);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedContacts(List<Contact> contacts) {
        try {
            int count = 0;
            for (Contact contact : contacts) {
                if (dbHelper.deleteContact(contact.getId())) {
                    count++;
                }
            }

            showSuccess(count + " contacts deleted");
            exitSelectionMode();
            loadContacts();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting contacts", e);
            showError("Error deleting contacts: " + e.getMessage());
        }
    }

    private void exitSelectionMode() {
        if (adapter.isInSelectionMode()) {
            adapter.toggleSelectionMode();
            updateMenuItemsVisibility();
            if (actionBar != null) {
                actionBar.setTitle("Contacts");
            }
        }
    }

    private void updateMenuItemsVisibility() {
        if (selectAllMenuItem != null && deleteMenuItem != null) {
            boolean visible = adapter != null && adapter.isInSelectionMode();
            selectAllMenuItem.setVisible(visible);
            deleteMenuItem.setVisible(visible);
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (actionBar != null) {
            if (count > 0) {
                actionBar.setTitle(count + " selected");
            } else {
                actionBar.setTitle("Contacts");
            }
        }
        updateMenuItemsVisibility();
    }

    private void loadContacts() {
        try {
            allContacts = dbHelper.getAllContacts();
            adapter.updateContacts(allContacts);

            // Update contact count
            if (contactCountView != null) {
                contactCountView.setText(getString(R.string.contact_count, allContacts.size()));
            }

            // Show/hide empty view
            if (allContacts.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                contactsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                contactsRecyclerView.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts", e);
            showError("Error loading contacts: " + e.getMessage());
        }
    }

    private void filterContacts(String query) {
        if (adapter != null) {
            adapter.filterContacts(allContacts, query);

            // Show/hide empty view based on filtered results
            if (adapter.getItemCount() == 0) {
                emptyView.setText("No contacts match your search");
                emptyView.setVisibility(View.VISIBLE);
                contactsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                contactsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error_color))
                    .show();
        }
    }

    private void showSuccess(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.success_color))
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadContacts();
    }

    @Override
    public void onPause() {
        super.onPause();
        exitSelectionMode();
    }

    // Implement ContactActionListener methods
    @Override
    public void onEditContact(Contact contact) {
        showEditContactDialog(contact);
    }

    @Override
    public void onDeleteContact(Contact contact) {
        try {
            if (dbHelper.deleteContact(contact.getId())) {
                showSuccess("Contact deleted");
                loadContacts();
            } else {
                showError("Failed to delete contact");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting contact", e);
            showError("Error deleting contact: " + e.getMessage());
        }
    }

    @Override
    public void onContactDetails(Contact contact) {
        // Show contact details dialog
        showContactDetailsDialog(contact);
    }

    private void showContactDetailsDialog(Contact contact) {
        // Create dialog view with contact details
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_contact_details, null);

        TextView detailsText = dialogView.findViewById(R.id.tvContactDetails);

        // Build details string
        StringBuilder details = new StringBuilder();
        for (String field : contact.getFieldKeys()) {
            String value = contact.getField(field);
            if (value != null && !value.isEmpty()) {
                details.append(field).append(": ").append(value).append("\n");
            }
        }

        detailsText.setText(details.toString());

        new AlertDialog.Builder(requireContext())
                .setTitle("Contact Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditContactDialog(contact))
                .setNegativeButton("Delete", (dialog, which) -> confirmAndDeleteContact(contact))
                .show();
    }

    private void confirmAndDeleteContact(Contact contact) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbHelper.deleteContact(contact.getId())) {
                        showSuccess("Contact deleted");
                        loadContacts();
                    } else {
                        showError("Failed to delete contact");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to edit an existing contact
     */
    private void showEditContactDialog(Contact contact) {
        // Create dialog view
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_contact, null);

        // Get references to input fields
        TextInputEditText nameInput = dialogView.findViewById(R.id.etName);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.etPhone);
        TextInputEditText emailInput = dialogView.findViewById(R.id.etEmail);

        // Pre-fill with existing data
        nameInput.setText(contact.getField("Name"));
        phoneInput.setText(contact.getField("Phone"));
        emailInput.setText(contact.getField("Email"));

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Contact")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Update contact fields
                    contact.setField("Name", nameInput.getText().toString().trim());
                    contact.setField("Phone", phoneInput.getText().toString().trim());
                    contact.setField("Email", emailInput.getText().toString().trim());

                    // Save to database
                    long id = dbHelper.saveContact(contact);
                    if (id > 0) {
                        showSuccess("Contact updated successfully");
                        loadContacts();
                    } else {
                        showError("Failed to update contact");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            // Clean up resources
            if (dbHelper != null) {
                dbHelper.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}
