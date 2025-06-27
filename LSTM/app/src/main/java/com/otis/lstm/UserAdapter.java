package com.otis.lstm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<DocumentSnapshot> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(DocumentSnapshot user);
    }

    public UserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<DocumentSnapshot> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        DocumentSnapshot user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName, textViewUserEmail, textViewUserContact, textViewUser2FA, textViewUserAdmin, textViewUserAddress, textViewUserBirthdate;
        Button buttonEditUser;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
            textViewUserContact = itemView.findViewById(R.id.textViewUserContact);
            textViewUser2FA = itemView.findViewById(R.id.textViewUser2FA);
            textViewUserAdmin = itemView.findViewById(R.id.textViewUserAdmin);
            textViewUserAddress = itemView.findViewById(R.id.textViewUserAddress);
            textViewUserBirthdate = itemView.findViewById(R.id.textViewUserBirthdate);
            buttonEditUser = itemView.findViewById(R.id.buttonEditUser);
        }

        void bind(DocumentSnapshot user, OnUserClickListener listener) {
            textViewUserName.setText("Name: " + (user.getString("fullName") != null ? user.getString("fullName") : "N/A"));
            textViewUserEmail.setText("Email: " + (user.getString("email") != null ? user.getString("email") : "N/A"));
            textViewUserContact.setText("Contact: " + (user.getString("contact") != null ? user.getString("contact") : "N/A"));
            Boolean enable2FA = user.getBoolean("enable2FA");
            textViewUser2FA.setText("2FA: " + (enable2FA != null && enable2FA ? "Enabled" : "Disabled"));
            Boolean admin = user.getBoolean("admin");
            textViewUserAdmin.setText("Admin: " + (admin != null && admin ? "Yes" : "No"));
            textViewUserAddress.setText("Address: " + (user.getString("address") != null ? user.getString("address") : "N/A"));
            textViewUserBirthdate.setText("Birthdate: " + (user.getString("birthdate") != null ? user.getString("birthdate") : "N/A"));
            buttonEditUser.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}