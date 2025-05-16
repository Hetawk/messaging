package com.andgroupco.messaging.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    public static final int PERMISSION_REQUEST_CODE = 123;
    private static final String TAG = "PermissionManager";

    private static List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Storage permissions based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);

            // Add notification permission for Android 13+
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // Add other required permissions
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.READ_SMS);

        return permissions;
    }

    public static boolean checkAndRequestPermissions(Activity activity) {
        List<String> permissions = getRequiredPermissions();
        List<String> permissionsToRequest = new ArrayList<>();
        List<String> permissionsToExplain = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);

                // Check if we should show explanation
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    permissionsToExplain.add(permission);
                }
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);

            if (!permissionsToExplain.isEmpty()) {
                // Show rationale before requesting
                showPermissionRationale(activity, permissionsToRequest);
                return false;
            }

            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        Log.d(TAG, "All permissions already granted");
        return true;
    }

    private static void showPermissionRationale(Activity activity, List<String> permissions) {
        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("This app needs permissions to access contacts and send messages. " +
                        "Please grant these permissions in the next dialog.")
                .setPositiveButton("OK", (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                            activity,
                            permissions.toArray(new String[0]),
                            PERMISSION_REQUEST_CODE);
                })
                .show();
    }

    public static void onPermissionResult(Activity activity, int requestCode,
            String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, permissions[0])) {
                // Show settings dialog if permissions permanently denied
                showSettingsDialog(activity);
            }
        }
    }

    public static boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                } else {
                    Log.d(TAG, "Permission granted: " + permissions[i]);
                }
            }
            return allGranted;
        }
        Log.w(TAG, "Permission request not handled: " + requestCode);
        return false;
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("Please enable permissions in Settings")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static boolean checkPermissionForOperation(Activity activity, String operation) {
        List<String> requiredPermissions = new ArrayList<>();

        switch (operation) {
            case "SMS":
                requiredPermissions.add(Manifest.permission.SEND_SMS);
                requiredPermissions.add(Manifest.permission.READ_SMS);
                break;
            case "CONTACTS":
                requiredPermissions.add(Manifest.permission.READ_CONTACTS);
                break;
            case "STORAGE":
                // Log which storage permission logic is being applied
                Log.d(TAG, "Checking storage permissions for Android SDK: " + Build.VERSION.SDK_INT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ uses granular media permissions
                    requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
                    requiredPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
                    Log.d(TAG, "Using Android 13+ (Tiramisu) permissions");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11-12 can use MANAGE_EXTERNAL_STORAGE for full access, but for most
                    // cases
                    // we should use Storage Access Framework instead of requesting broad
                    // permissions
                    // This permission requires special handling and isn't granted via normal
                    // permission flow
                    Log.d(TAG, "Using Android 11-12 (R+) - SAF approach recommended");

                    // For file pickers, we don't really need storage permissions with SAF
                    return true;
                } else {
                    // For Android 10 and below
                    requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    Log.d(TAG, "Using Android 10 and below storage permissions");
                }
                break;
            case "NOTIFICATIONS":
                // Only request notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
                    Log.d(TAG, "Checking notification permission for Android 13+");
                } else {
                    // Notifications don't require runtime permission before Android 13
                    Log.d(TAG, "Notification permission not required for this Android version");
                    return true;
                }
                break;
            default:
                Log.w(TAG, "Unknown operation type: " + operation);
                return false;
        }

        // Check if permissions are already granted
        boolean allGranted = true;
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : requiredPermissions) {
            int result = ContextCompat.checkSelfPermission(activity, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + permission);
                allGranted = false;
                permissionsToRequest.add(permission);
            } else {
                Log.d(TAG, "Permission already granted: " + permission);
            }
        }

        if (!allGranted && !permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        Log.d(TAG, "All permissions granted for operation: " + operation);
        return true;
    }
}
