package com.andgroupco.messaging.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.andgroupco.messaging.R;
import com.andgroupco.messaging.base.BaseActivity;
import com.andgroupco.messaging.models.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private final Context context;

    public MessageListAdapter(Context context) {
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView contentView;
        public TextView statusView;
        public TextView recipientView;

        public ViewHolder(View view) {
            super(view);
            contentView = view.findViewById(R.id.messageContent);
            statusView = view.findViewById(R.id.messageStatus);
            recipientView = view.findViewById(R.id.messageRecipient);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.contentView.setText(message.getContent());
        holder.recipientView.setText(message.getRecipient());
        holder.statusView.setText(message.getStatus());

        holder.itemView.setOnLongClickListener(v -> {
            showCopyOptions(message);
            return true;
        });
    }

    private void showCopyOptions(Message message) {
        new AlertDialog.Builder(context)
                .setTitle("Copy")
                .setItems(new String[] { "Copy Message", "Copy Recipient" }, (dialog, which) -> {
                    if (which == 0) {
                        ((BaseActivity) context).copyToClipboard(
                                message.getContent(), "Message Content");
                    } else {
                        ((BaseActivity) context).copyToClipboard(
                                message.getRecipient(), "Recipient");
                    }
                })
                .show();
    }

    // Fix the updateMessages method to specify Message type explicitly
    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
}
