package com.andgroupco.messaging.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andgroupco.messaging.R;
import com.andgroupco.messaging.adapters.MessageListAdapter;
import com.andgroupco.messaging.models.Message;
import com.andgroupco.messaging.services.MessageService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {
    private static final String TAG = "HistoryFragment";
    private RecyclerView messagesList;
    private TabLayout historyTabs;
    private MessageListAdapter adapter;
    private MessageService messageService;

    public HistoryFragment() {
        // Required empty public constructor
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize views and services
            messagesList = view.findViewById(R.id.messagesList);
            historyTabs = view.findViewById(R.id.historyTabs);
            messageService = new MessageService(getContext());

            // Set up RecyclerView
            messagesList.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new MessageListAdapter(getContext());
            messagesList.setAdapter(adapter);

            // Set up tab listener
            historyTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    loadMessages(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // Not needed
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // Refresh on reselect
                    loadMessages(tab.getPosition());
                }
            });

            // Load initial data
            loadMessages(0);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up History fragment", e);
        }
    }

    private void loadMessages(int tabPosition) {
        try {
            if (adapter == null || messageService == null) {
                Log.e(TAG, "Adapter or MessageService is null");
                return;
            }

            // In a real app, this would filter by message type based on tab
            List<Message> messages = messageService.getAllMessages();

            // Make sure we're using a properly typed list
            List<Message> safeMessageList = messages != null ? messages : new ArrayList<>();
            adapter.updateMessages(safeMessageList);
        } catch (Exception e) {
            Log.e(TAG, "Error loading messages", e);
        }
    }

    // Add this method to allow refreshing from outside
    public void refreshMessages() {
        if (isAdded() && historyTabs != null) {
            loadMessages(historyTabs.getSelectedTabPosition());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh messages when fragment becomes visible
        refreshMessages();
    }
}
