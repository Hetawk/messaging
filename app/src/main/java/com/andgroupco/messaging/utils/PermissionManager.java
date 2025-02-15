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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
                    requiredPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
                } else {
                    requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                break;
            default:
                return false;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions for " + operation + ": " + permissionsToRequest);
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }
}
