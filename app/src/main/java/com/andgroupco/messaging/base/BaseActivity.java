package com.andgroupco.messaging.base;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.andgroupco.messaging.R;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    protected void showError(String message) {
        Log.e(TAG, "Error: " + message);
        try {
            View contentView = findViewById(android.R.id.content);
            if (contentView != null && !isFinishing()) {
                Snackbar.make(contentView, message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.error_color))
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show error snackbar", e);
            // Fallback to Toast if Snackbar fails
            try {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Log.e(TAG, "Failed to show error toast", e2);
            }
        }
    }

    protected void showSuccess(String message) {
        Log.d(TAG, "Success: " + message);
        try {
            View contentView = findViewById(android.R.id.content);
            if (contentView != null && !isFinishing()) {
                Snackbar.make(contentView, message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.success_color))
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show success snackbar", e);
            // Fallback to Toast
            try {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Log.e(TAG, "Failed to show success toast", e2);
            }
        }
    }

    public void copyToClipboard(String text, String label) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && text != null) {
                ClipData clip = ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip);
                showSuccess("Copied to clipboard");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy to clipboard", e);
            showError("Could not copy text");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save any necessary state
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore any necessary state
    }
}
