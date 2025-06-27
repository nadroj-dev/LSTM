package com.otis.lstm;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class ProfileDetailsActivity extends AppCompatActivity {

    private TextView fullNameTextView, contactTextView, emailTextView, passwordTextView, addressTextView, birthdateTextView;
    private ImageView editNameIcon, editContactIcon, editEmailIcon, editPasswordIcon, editAddressIcon, editBirthdateIcon;
    private Button editProfilePictureButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private static final Pattern BIRTHDATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_details);

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        fullNameTextView = findViewById(R.id.fullNameTextView);
        contactTextView = findViewById(R.id.contactTextView);
        emailTextView = findViewById(R.id.emailTextView);
        passwordTextView = findViewById(R.id.passwordTextView);
        addressTextView = findViewById(R.id.addressTextView);
        birthdateTextView = findViewById(R.id.birthdateTextView);
        editNameIcon = findViewById(R.id.editNameIcon);
        editContactIcon = findViewById(R.id.editContactIcon);
        editEmailIcon = findViewById(R.id.editEmailIcon);
        editPasswordIcon = findViewById(R.id.editPasswordIcon);
        editAddressIcon = findViewById(R.id.editAddressIcon);
        editBirthdateIcon = findViewById(R.id.editBirthdateIcon);
        editProfilePictureButton = findViewById(R.id.editProfilePictureButton);

        // Load user details
        loadUserDetails();

        // Set click listeners for edit icons
        editNameIcon.setOnClickListener(v -> showEditDialog("Full Name", fullNameTextView, "fullName"));
        editContactIcon.setOnClickListener(v -> showEditDialog("Contact Number", contactTextView, "contact"));
        editEmailIcon.setOnClickListener(v -> showEditDialog("Email", emailTextView, "email"));
        editPasswordIcon.setOnClickListener(v -> showPasswordResetDialog());
        editAddressIcon.setOnClickListener(v -> showEditDialog("Address", addressTextView, "address"));
        editBirthdateIcon.setOnClickListener(v -> showBirthdateEditDialog());
        // Placeholder for profile picture editing
        editProfilePictureButton.setOnClickListener(v -> {
            Toast.makeText(this, "Profile picture editing not implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserDetails() {
        if (currentUser == null) {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch user details from Firestore
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Populate fields with user data
                            fullNameTextView.setText(document.getString("fullName"));
                            contactTextView.setText(document.getString("contact"));
                            emailTextView.setText(document.getString("email"));
                            addressTextView.setText(document.getString("address"));
                            birthdateTextView.setText(document.getString("birthdate"));
                            // Password is not stored in Firestore; keep it as dots
                            passwordTextView.setText("••••••••••••");
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching user data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEditDialog(String fieldName, TextView textView, String firestoreField) {
        final EditText input = new EditText(this);
        input.setText(textView.getText().toString());
        if (firestoreField.equals("contact")) {
            input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else if (firestoreField.equals("email")) {
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit " + fieldName)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newValue = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newValue)) {
                        Toast.makeText(this, fieldName + " cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update Firestore
                    db.collection("Users")
                            .document(currentUser.getUid())
                            .update(firestoreField, newValue)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    textView.setText(newValue);
                                    Toast.makeText(this, fieldName + " updated successfully", Toast.LENGTH_SHORT).show();

                                    // If email is updated, update Firebase Auth email as well
                                    if (firestoreField.equals("email")) {
                                        currentUser.verifyBeforeUpdateEmail(newValue)
                                                .addOnCompleteListener(emailTask -> {
                                                    if (!emailTask.isSuccessful()) {
                                                        Toast.makeText(this, "Failed to update email in Firebase Auth: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(this, "Verification email sent for new email address", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to update " + fieldName + ": " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showBirthdateEditDialog() {
        // Parse current birthdate or use today as default
        Calendar calendar = Calendar.getInstance();
        String currentBirthdate = birthdateTextView.getText().toString();
        if (!TextUtils.isEmpty(currentBirthdate) && BIRTHDATE_PATTERN.matcher(currentBirthdate).matches()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                Date date = sdf.parse(currentBirthdate);
                calendar.setTime(date);
            } catch (ParseException e) {
                // Fallback to today if parsing fails
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String newBirthdate = String.format("%02d/%02d/%04d", selectedMonth + 1, selectedDay, selectedYear);

                    // Validate birthdate
                    if (!BIRTHDATE_PATTERN.matcher(newBirthdate).matches()) {
                        Toast.makeText(this, "Invalid birthdate format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    sdf.setLenient(false);
                    try {
                        Date birthDate = sdf.parse(newBirthdate);
                        Calendar birthCal = Calendar.getInstance();
                        birthCal.setTime(birthDate);
                        Calendar minAgeCal = Calendar.getInstance();
                        minAgeCal.add(Calendar.YEAR, -13);
                        if (birthCal.after(minAgeCal)) {
                            Toast.makeText(this, "You must be at least 13 years old", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update Firestore
                        db.collection("Users")
                                .document(currentUser.getUid())
                                .update("birthdate", newBirthdate)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        birthdateTextView.setText(newBirthdate);
                                        Toast.makeText(this, "Birthdate updated successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Failed to update birthdate: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } catch (ParseException e) {
                        Toast.makeText(this, "Invalid birthdate", Toast.LENGTH_SHORT).show();
                    }
                },
                year, month, day
        );
        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showPasswordResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("A password reset link will be sent to your email: " + currentUser.getEmail())
                .setPositiveButton("Send", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(currentUser.getEmail())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}