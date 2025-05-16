package com.andgroupco.messaging.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.andgroupco.messaging.ComposeMessageActivity;
import com.andgroupco.messaging.MainActivity;
import com.andgroupco.messaging.R;
import com.andgroupco.messaging.services.MessageService;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView tvRecentActivity;
    private Button btnImportContacts;
    private Button btnComposeMessage;
    private MessageService messageService;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize message service
            messageService = new MessageService(getContext());

            // Initialize views
            tvRecentActivity = view.findViewById(R.id.tvRecentActivity);
            btnImportContacts = view.findViewById(R.id.btnImportContacts);
            btnComposeMessage = view.findViewById(R.id.btnComposeMessage);

            // Set up click listeners
            if (btnImportContacts != null) {
                btnImportContacts.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showImportOptions();
                    }
                });
            }

            if (btnComposeMessage != null) {
                btnComposeMessage.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ComposeMessageActivity.class);
                    startActivity(intent);
                });
            }

            // Load recent activity
            loadRecentActivity();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Home fragment", e);
        }
    }

    private void loadRecentActivity() {
        try {
            if (tvRecentActivity == null)
                return;

            // Try to get some recent messages
            if (messageService != null) {
                // Get the most recent message or show default text
                tvRecentActivity.setText("No recent activity to display");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recent activity", e);
        }
    }

    // Add this method to allow refreshing from outside
    public void refreshData() {
        if (isAdded()) {
            loadRecentActivity();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        refreshData();
    }
}
