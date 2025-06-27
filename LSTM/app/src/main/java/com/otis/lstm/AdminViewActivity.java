package com.otis.lstm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminViewActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewFullName, textViewEmail, textViewContact, textView2FAStatus;
    private Button buttonViewMonitoring, buttonManageUsers, buttonSignOut;
    private static final Pattern BIRTHDATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewFullName = findViewById(R.id.textViewFullName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewContact = findViewById(R.id.textViewContact);
        textView2FAStatus = findViewById(R.id.textView2FAStatus);
        buttonViewMonitoring = findViewById(R.id.buttonViewMonitoring);
        buttonManageUsers = findViewById(R.id.buttonManageUsers);
        buttonSignOut = findViewById(R.id.buttonSignOut);

        loadUserData();

        buttonViewMonitoring.setOnClickListener(v -> {
            Intent intent = new Intent(AdminViewActivity.this, MainActivity.class);
            startActivity(intent);
        });

        buttonManageUsers.setOnClickListener(v -> showManageUsersDialog());

        buttonSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminViewActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("Users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                textViewFullName.setText("Full Name: " + document.getString("fullName"));
                                textViewEmail.setText("Email: " + document.getString("email"));
                                textViewContact.setText("Contact: " + document.getString("contact"));
                                textView2FAStatus.setText("2FA: " + (document.getBoolean("enable2FA") ? "Enabled" : "Disabled"));
                            } else {
                                Toast.makeText(AdminViewActivity.this,
                                        "User data not found",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AdminViewActivity.this,
                                    "Error loading user data: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showManageUsersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_user, null);
        builder.setView(dialogView);

        RecyclerView recyclerViewUsers = dialogView.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        UserAdapter userAdapter = new UserAdapter(user -> showEditUserDialog(user));
        recyclerViewUsers.setAdapter(userAdapter);

        Button buttonAddUser = dialogView.findViewById(R.id.buttonAddUser);
        buttonAddUser.setOnClickListener(v -> showAddUserDialog());

        db.collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> users = task.getResult().getDocuments();
                        userAdapter.setUsers(users);
                    } else {
                        Toast.makeText(AdminViewActivity.this,
                                "Error loading users: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        Button buttonClose = dialogView.findViewById(R.id.buttonClose);
        AlertDialog dialog = builder.create();
        buttonClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(dialogView);

        EditText editTextName = dialogView.findViewById(R.id.editTextName);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextContact = dialogView.findViewById(R.id.editTextContact);
        EditText editTextAddress = dialogView.findViewById(R.id.editTextAddress);
        EditText editTextBirthdate = dialogView.findViewById(R.id.editTextBirthdate);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = builder.create();

        buttonSave.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String contact = editTextContact.getText().toString().trim();
            String address = editTextAddress.getText().toString().trim();
            String birthdate = editTextBirthdate.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(contact) ||
                    TextUtils.isEmpty(address) || TextUtils.isEmpty(birthdate)) {
                Toast.makeText(AdminViewActivity.this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!BIRTHDATE_PATTERN.matcher(birthdate).matches()) {
                Toast.makeText(AdminViewActivity.this,
                        "Invalid birthdate format (MM/DD/YYYY)",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            sdf.setLenient(false);
            try {
                Date birthDate = sdf.parse(birthdate);
                Calendar birthCal = Calendar.getInstance();
                birthCal.setTime(birthDate);
                Calendar minAgeCal = Calendar.getInstance();
                minAgeCal.add(Calendar.YEAR, -13);
                if (birthCal.after(minAgeCal)) {
                    Toast.makeText(AdminViewActivity.this,
                            "User must be at least 13 years old",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                Toast.makeText(AdminViewActivity.this,
                        "Invalid birthdate",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> user = new HashMap<>();
            user.put("fullName", name);
            user.put("email", email);
            user.put("contact", contact);
            user.put("address", address);
            user.put("birthdate", birthdate);
            user.put("enable2FA", false);
            user.put("admin", false);

            db.collection("Users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AdminViewActivity.this,
                                "User added successfully",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        showManageUsersDialog();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminViewActivity.this,
                            "Error adding user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditUserDialog(DocumentSnapshot user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        EditText editTextName = dialogView.findViewById(R.id.editTextName);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextContact = dialogView.findViewById(R.id.editTextContact);
        EditText editTextAddress = dialogView.findViewById(R.id.editTextAddress);
        EditText editTextBirthdate = dialogView.findViewById(R.id.editTextBirthdate);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        editTextName.setText(user.getString("fullName"));
        editTextEmail.setText(user.getString("email"));
        editTextContact.setText(user.getString("contact"));
        editTextAddress.setText(user.getString("address"));
        editTextBirthdate.setText(user.getString("birthdate"));

        AlertDialog dialog = builder.create();

        buttonSave.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String contact = editTextContact.getText().toString().trim();
            String address = editTextAddress.getText().toString().trim();
            String birthdate = editTextBirthdate.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(contact) ||
                    TextUtils.isEmpty(address) || TextUtils.isEmpty(birthdate)) {
                Toast.makeText(AdminViewActivity.this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!BIRTHDATE_PATTERN.matcher(birthdate).matches()) {
                Toast.makeText(AdminViewActivity.this,
                        "Invalid birthdate format (MM/DD/YYYY)",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            sdf.setLenient(false);
            try {
                Date birthDate = sdf.parse(birthdate);
                Calendar birthCal = Calendar.getInstance();
                birthCal.setTime(birthDate);
                Calendar minAgeCal = Calendar.getInstance();
                minAgeCal.add(Calendar.YEAR, -13);
                if (birthCal.after(minAgeCal)) {
                    Toast.makeText(AdminViewActivity.this,
                            "User must be at least 13 years old",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                Toast.makeText(AdminViewActivity.this,
                        "Invalid birthdate",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", name);
            updates.put("email", email);
            updates.put("contact", contact);
            updates.put("address", address);
            updates.put("birthdate", birthdate);

            db.collection("Users").document(user.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminViewActivity.this,
                                "User updated successfully",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        showManageUsersDialog();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminViewActivity.this,
                            "Error updating user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}