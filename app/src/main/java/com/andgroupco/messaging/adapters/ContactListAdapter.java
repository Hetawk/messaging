package com.andgroupco.messaging.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.andgroupco.messaging.R;
import com.andgroupco.messaging.base.BaseActivity;
import com.andgroupco.messaging.models.Contact;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {
    private List<Contact> contacts = new ArrayList<>();
    private final Context context;
    private Set<String> visibleFields = new LinkedHashSet<>();

    // Priority fields to display first if available
    private static final String[] PRIORITY_FIELDS = {
            "Name", "Phone", "WhatsApp", "Email", "Country"
    };

    private boolean selectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private ContactSelectionListener selectionListener;
    private ContactActionListener actionListener;

    public ContactListAdapter(Context context) {
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameView;
        public TextView detailsView;

        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.contact_name);
            detailsView = view.findViewById(R.id.contact_details);
        }
    }

    public interface ContactSelectionListener {
        void onSelectionChanged(int count);
    }

    public void setSelectionListener(ContactSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void toggleSelectionMode() {
        selectionMode = !selectionMode;
        if (!selectionMode) {
            clearSelections();
        }
        notifyDataSetChanged();
    }

    public boolean isInSelectionMode() {
        return selectionMode;
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < contacts.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelections() {
        selectedPositions.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    public List<Contact> getSelectedContacts() {
        List<Contact> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos < contacts.size()) {
                selected.add(contacts.get(pos));
            }
        }
        return selected;
    }

    public interface ContactActionListener {
        void onEditContact(Contact contact);

        void onDeleteContact(Contact contact);

        void onContactDetails(Contact contact);
    }

    public void setActionListener(ContactActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Set selection background if in selection mode
        if (selectionMode) {
            if (selectedPositions.contains(position)) {
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.tertiary_light));
            } else {
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            }
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        // Set name or first available field as primary text
        String nameField = contact.getField("Name");
        if (nameField != null && !nameField.isEmpty()) {
            holder.nameView.setText(nameField);
        } else {
            // Find first non-empty field
            for (String field : visibleFields) {
                String value = contact.getField(field);
                if (value != null && !value.isEmpty()) {
                    holder.nameView.setText(field + ": " + value);
                    break;
                }
            }
        }

        // Build details text
        StringBuilder details = new StringBuilder();
        for (String field : visibleFields) {
            // Skip name field as it's already shown
            if (field.equals("Name"))
                continue;

            String value = contact.getField(field);
            if (value != null && !value.isEmpty()) {
                if (details.length() > 0) {
                    details.append("\n");
                }
                details.append(field).append(": ").append(value);
            }
        }

        holder.detailsView.setText(details.toString());

        // Update click handling based on selection mode
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(position);
            } else {
                if (actionListener != null) {
                    actionListener.onContactDetails(contacts.get(position));
                } else {
                    showContactDetails(contacts.get(position));
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                toggleSelectionMode();
                toggleSelection(position);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    private void showContactDetails(Contact contact) {
        // Build full details
        StringBuilder details = new StringBuilder();
        for (String field : contact.getFieldKeys()) {
            String value = contact.getField(field);
            if (value != null && !value.isEmpty()) {
                details.append(field).append(": ").append(value).append("\n");
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Contact Details")
                .setMessage(details.toString())
                .setPositiveButton("Close", null);

        // Add action buttons if contact has phone or email
        String phone = contact.getField("Phone");
        String email = contact.getField("Email");
        String whatsapp = contact.getField("WhatsApp");

        // Add Edit button
        builder.setNeutralButton("Edit", (dialog, which) -> {
            if (actionListener != null) {
                actionListener.onEditContact(contact);
            }
        });

        // Add Delete button
        builder.setNegativeButton("Delete", (dialog, which) -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setPositiveButton("Delete", (confirmDialog, confirmWhich) -> {
                        if (actionListener != null) {
                            actionListener.onDeleteContact(contact);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Add Contact Action buttons in a custom view below dialog
        if (phone != null && !phone.isEmpty() || email != null && !email.isEmpty() ||
                whatsapp != null && !whatsapp.isEmpty()) {
            // This would be better with a custom dialog layout
            dialog.setOnDismissListener(d -> showContactActionOptions(contact));
        }
    }

    private void showContactActionOptions(Contact contact) {
        String phone = contact.getField("Phone");
        String email = contact.getField("Email");
        String whatsapp = contact.getField("WhatsApp");

        List<String> optionsList = new ArrayList<>();

        if (phone != null && !phone.isEmpty())
            optionsList.add("Call");
        if (email != null && !email.isEmpty())
            optionsList.add("Email");
        if (whatsapp != null && !whatsapp.isEmpty())
            optionsList.add("WhatsApp");
        optionsList.add("Copy Contact");

        // Create a final array to use in the lambda
        final String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(context)
                .setTitle("Contact Actions")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    switch (selected) {
                        case "Call":
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:" + phone));
                            context.startActivity(callIntent);
                            break;
                        case "Email":
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:" + email));
                            context.startActivity(emailIntent);
                            break;
                        case "WhatsApp":
                            try {
                                Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + whatsapp);
                                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW, uri);
                                context.startActivity(whatsappIntent);
                            } catch (Exception e) {
                                // Fallback
                                Uri uri = Uri.parse("https://wa.me/" + whatsapp);
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                                context.startActivity(browserIntent);
                            }
                            break;
                        case "Copy Contact":
                            if (context instanceof BaseActivity) {
                                StringBuilder contactDetails = new StringBuilder();
                                for (String field : contact.getFieldKeys()) {
                                    String value = contact.getField(field);
                                    if (value != null && !value.isEmpty()) {
                                        contactDetails.append(field).append(": ").append(value).append("\n");
                                    }
                                }
                                ((BaseActivity) context).copyToClipboard(contactDetails.toString(), "Contact Details");
                            }
                            break;
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    /**
     * Update the contacts list and determine visible fields
     *
     * @param contacts list of contacts
     */
    public void updateContacts(List<Contact> contacts) {
        this.contacts = contacts;

        // Determine visible fields from all contacts
        visibleFields.clear();

        // First add priority fields that exist in any contact
        for (String field : PRIORITY_FIELDS) {
            for (Contact contact : contacts) {
                if (!contact.getField(field).isEmpty()) {
                    visibleFields.add(field);
                    break;
                }
            }
        }

        // Then add any other fields
        for (Contact contact : contacts) {
            for (String field : contact.getFieldKeys()) {
                if (!visibleFields.contains(field)) {
                    visibleFields.add(field);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Filter contacts by search query
     *
     * @param allContacts full list of contacts
     * @param query       search query
     */
    public void filterContacts(List<Contact> allContacts, String query) {
        if (query == null || query.isEmpty()) {
            updateContacts(allContacts);
            return;
        }

        List<Contact> filteredList = new ArrayList<>();
        for (Contact contact : allContacts) {
            if (contact.matchesSearch(query)) {
                filteredList.add(contact);
            }
        }

        updateContacts(filteredList);
    }
}
