package com.otis.lstm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFullName, editTextEmail, editTextContact, editTextAddress, editTextBirthdate, editTextPassword;
    private CheckBox checkBox2FA;
    private Button buttonSignIn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Regular expressions for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?\\d{10,15}$"
    );
    private static final Pattern BIRTHDATE_PATTERN = Pattern.compile(
            "^\\d{2}/\\d{2}/\\d{4}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextContact = findViewById(R.id.editTextContact);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextBirthdate = findViewById(R.id.editTextBirthdate);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBox2FA = findViewById(R.id.checkBox2FA);
        buttonSignIn = findViewById(R.id.buttonSignIn);

        // Set up DatePickerDialog for birthdate
        editTextBirthdate.setOnClickListener(v -> showDatePickerDialog());

        // Set click listener for register button
        buttonSignIn.setOnClickListener(v -> registerUser());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedMonth + 1, selectedDay, selectedYear);
                    editTextBirthdate.setText(formattedDate);
                },
                year, month, day
        );
        // Set maximum date to today to prevent future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String contact = editTextContact.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String birthdate = editTextBirthdate.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        boolean enable2FA = checkBox2FA.isChecked();

        // Validation
        if (fullName.isEmpty()) {
            editTextFullName.setError("Full name is required");
            editTextFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            editTextEmail.setError("Invalid email format");
            editTextEmail.requestFocus();
            return;
        }

        if (contact.isEmpty()) {
            editTextContact.setError("Contact number is required");
            editTextContact.requestFocus();
            return;
        }

        if (!PHONE_PATTERN.matcher(contact).matches()) {
            editTextContact.setError("Invalid phone number format (e.g., +1234567890 or 1234567890)");
            editTextContact.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            editTextAddress.setError("Address is required");
            editTextAddress.requestFocus();
            return;
        }

        if (birthdate.isEmpty()) {
            editTextBirthdate.setError("Birthdate is required");
            editTextBirthdate.requestFocus();
            return;
        }

        if (!BIRTHDATE_PATTERN.matcher(birthdate).matches()) {
            editTextBirthdate.setError("Invalid birthdate format (MM/DD/YYYY)");
            editTextBirthdate.requestFocus();
            return;
        }

        // Validate age (must be at least 13 years old)
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        sdf.setLenient(false);
        try {
            Date birthDate = sdf.parse(birthdate);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar minAgeCal = Calendar.getInstance();
            minAgeCal.add(Calendar.YEAR, -13);
            if (birthCal.after(minAgeCal)) {
                editTextBirthdate.setError("You must be at least 13 years old");
                editTextBirthdate.requestFocus();
                return;
            }
        } catch (ParseException e) {
            editTextBirthdate.setError("Invalid birthdate");
            editTextBirthdate.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        // Check if email already exists in Firestore
        checkEmailExists(email, emailExists -> {
            if (emailExists) {
                editTextEmail.setError("Email is already registered");
                editTextEmail.requestFocus();
                return;
            }

            // Check if phone number already exists in Firestore
            checkPhoneExists(contact, phoneExists -> {
                if (phoneExists) {
                    editTextContact.setError("Phone number is already registered");
                    editTextContact.requestFocus();
                    return;
                }

                // Proceed with user registration
                createUser(fullName, email, contact, address, birthdate, password, enable2FA);
            });
        });
    }

    private void checkEmailExists(String email, OnCheckCompleteListener listener) {
        db.collection("Users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        listener.onComplete(!querySnapshot.isEmpty());
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Error checking email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        listener.onComplete(false);
                    }
                });
    }

    private void checkPhoneExists(String contact, OnCheckCompleteListener listener) {
        db.collection("Users")
                .whereEqualTo("contact", contact)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        listener.onComplete(!querySnapshot.isEmpty());
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Error checking phone number: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        listener.onComplete(false);
                    }
                });
    }

    private void createUser(String fullName, String email, String contact, String address, String birthdate, String password, boolean enable2FA) {
        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Save user details to Firestore
                        saveUserDetailsToFirestore(user, fullName, email, contact, address, birthdate, enable2FA);

                        if (enable2FA) {
                            sendEmailVerification(user, fullName);
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration successful",
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDetailsToFirestore(FirebaseUser user, String fullName, String email, String contact, String address, String birthdate, boolean enable2FA) {
        // Create a user map to store in Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("contact", contact);
        userData.put("address", address);
        userData.put("birthdate", birthdate);
        userData.put("enable2FA", enable2FA);
        userData.put("emailVerified", false);
        userData.put("admin", false);

        // Save to "Users" collection with UID as document ID
        db.collection("Users")
                .document(user.getUid())
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this,
                                "User details saved successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Failed to save user details: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, String fullName) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this,
                                "Verification email sent to " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                        // Sign out the user until they verify their email
                        mAuth.signOut();
                        // Redirect to LoginActivity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Failed to send verification email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (currentUser.isEmailVerified()) {
                    // Update emailVerified field in Firestore
                    db.collection("Users")
                            .document(currentUser.getUid())
                            .update("emailVerified", true)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this,
                                            "Email verified successfully",
                                            Toast.LENGTH_SHORT).show();
                                    // Redirect to LoginActivity
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            });
        }
    }

    // Interface for handling async Firestore checks
    private interface OnCheckCompleteListener {
        void onComplete(boolean exists);
    }
}