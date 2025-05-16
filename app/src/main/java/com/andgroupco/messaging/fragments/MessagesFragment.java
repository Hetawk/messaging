package com.andgroupco.messaging.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andgroupco.messaging.ComposeMessageActivity;
import com.andgroupco.messaging.R;

public class MessagesFragment extends Fragment {
    private static final String TAG = "MessagesFragment";
    private RecyclerView templatesList;
    private Button btnNewMessage;

    public MessagesFragment() {
        // Required empty public constructor
    }

    public static MessagesFragment newInstance() {
        return new MessagesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize views
            templatesList = view.findViewById(R.id.templatesList);
            btnNewMessage = view.findViewById(R.id.btnNewMessage);

            // Set up RecyclerView
            if (templatesList != null) {
                templatesList.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            // Set up click listener for compose button
            if (btnNewMessage != null) {
                btnNewMessage.setOnClickListener(v -> launchComposeActivity());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up views", e);
        }
    }

    private void launchComposeActivity() {
        try {
            Intent intent = new Intent(getActivity(), ComposeMessageActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching compose activity", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh templates when fragment becomes visible
        loadTemplates();
    }

    private void loadTemplates() {
        // This would load message templates from the database
        // For now, it's just a placeholder
    }
}
