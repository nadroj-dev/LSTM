package com.otis.lstm;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private Button forgotPasswordBtn, profileSettingsBtn, reportBtn, configBtn, customizeAlertsBtn, logoutBtn, changeSecurityQuestionsBtn, viewAdminBtn, logsBtn, addDeviceBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        forgotPasswordBtn = findViewById(R.id.changePasswordBtn);
        profileSettingsBtn = findViewById(R.id.profileSettingsBtn);
        reportBtn = findViewById(R.id.reportBtn);
        configBtn = findViewById(R.id.configBtn);
        customizeAlertsBtn = findViewById(R.id.customizeAlertsBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        changeSecurityQuestionsBtn = findViewById(R.id.changeSecurityQuestionsBtn);
        viewAdminBtn = findViewById(R.id.viewAdminBtn);
        logsBtn = findViewById(R.id.logsBtn);
        addDeviceBtn = findViewById(R.id.addDeviceBtn);

        // Check admin status and show/hide View Admin button
        checkAdminStatus();

        forgotPasswordBtn.setOnClickListener(v -> showForgotPasswordDialog());

        profileSettingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ProfileDetailsActivity.class);
            startActivity(intent);
        });

        reportBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ReportsActivity.class);
            startActivity(intent);
        });

        configBtn.setOnClickListener(view -> showConfigDialog());

        customizeAlertsBtn.setOnClickListener(view -> showCustomizeAlertsDialog());

        changeSecurityQuestionsBtn.setOnClickListener(view -> showSecuritySettingsDialog());

        viewAdminBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AdminViewActivity.class);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(view -> showLogoutConfirmationDialog());

        logsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LogsActivity.class);
            startActivity(intent);
        });
        addDeviceBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
        });
    }

    private void checkAdminStatus() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("Users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Boolean isAdmin = task.getResult().getBoolean("admin");
                            if (isAdmin != null && isAdmin) {
                                viewAdminBtn.setVisibility(Button.VISIBLE);
                            } else {
                                viewAdminBtn.setVisibility(Button.GONE);
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this,
                                    "Error checking admin status: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showForgotPasswordDialog() {
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Please enter your email address:")
                .setView(emailInput)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(ProfileActivity.this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        sendPasswordResetEmail(email);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showConfigDialog() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        final TextView tempLabel = new TextView(this);
        tempLabel.setText("Temperature Threshold (°C)");
        tempLabel.setPadding(16, 16, 16, 8);

        final EditText tempInput = new EditText(this);
        tempInput.setHint("Enter Temperature Threshold (°C)");
        tempInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Create TextView for Humidity label
        final TextView humidityLabel = new TextView(this);
        humidityLabel.setText("Humidity Threshold (%)");
        humidityLabel.setPadding(16, 8, 16, 8);

        final EditText humidityInput = new EditText(this);
        humidityInput.setHint("Enter Humidity Threshold (%)");
        humidityInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(tempLabel);
        layout.addView(tempInput);
        layout.addView(humidityLabel);
        layout.addView(humidityInput);

        database.child("humidThreshold").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Long humidThreshold = dataSnapshot.getValue(Long.class);
                    humidityInput.setText(String.valueOf(humidThreshold));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error retrieving data", Toast.LENGTH_SHORT).show();
            }
        });

        database.child("tempThreshold").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Long tempThreshold = dataSnapshot.getValue(Long.class);
                    tempInput.setText(String.valueOf(tempThreshold));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error retrieving data", Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Configure Thresholds")
                .setMessage("Set the desired temperature and humidity thresholds:")
                .setView(layout)
                .setPositiveButton("Change Threshold", (dialog, which) -> {
                    String tempText = tempInput.getText().toString().trim();
                    String humidityText = humidityInput.getText().toString().trim();

                    if (TextUtils.isEmpty(tempText) || TextUtils.isEmpty(humidityText)) {
                        Toast.makeText(ProfileActivity.this, "Please enter both thresholds", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            float tempThreshold = Float.parseFloat(tempText);
                            float humidityThreshold = Float.parseFloat(humidityText);

                            database.child("tempThreshold").setValue(tempThreshold);
                            database.child("humidThreshold").setValue(humidityThreshold);

                            Toast.makeText(ProfileActivity.this, "Thresholds updated:\nTemperature: " + tempThreshold + "°C\nHumidity: " + humidityThreshold + "%", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } catch (NumberFormatException e) {
                            Toast.makeText(ProfileActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showCustomizeAlertsDialog() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        final CheckBox tempAlertCheckbox = new CheckBox(this);
        tempAlertCheckbox.setText("Enable Temperature Alerts");

        final CheckBox humidityAlertCheckbox = new CheckBox(this);
        humidityAlertCheckbox.setText("Enable Humidity Alerts");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(tempAlertCheckbox);
        layout.addView(humidityAlertCheckbox);

        database.child("alertSettings").child("tempAlertEnabled").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isEnabled = dataSnapshot.getValue(Boolean.class);
                    tempAlertCheckbox.setChecked(isEnabled != null && isEnabled);
                } else {
                    tempAlertCheckbox.setChecked(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error retrieving temperature alert settings", Toast.LENGTH_SHORT).show();
            }
        });

        database.child("alertSettings").child("humidityAlertEnabled").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isEnabled = dataSnapshot.getValue(Boolean.class);
                    humidityAlertCheckbox.setChecked(isEnabled != null && isEnabled);
                } else {
                    humidityAlertCheckbox.setChecked(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error retrieving humidity alert settings", Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Customize Alerts")
                .setMessage("Select which alerts to enable:")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean tempAlertEnabled = tempAlertCheckbox.isChecked();
                    boolean humidityAlertEnabled = humidityAlertCheckbox.isChecked();

                    database.child("alertSettings").child("tempAlertEnabled").setValue(tempAlertEnabled);
                    database.child("alertSettings").child("humidityAlertEnabled").setValue(humidityAlertEnabled);

                    Toast.makeText(ProfileActivity.this,
                            "Alert settings updated:\nTemperature Alerts: " + (tempAlertEnabled ? "Enabled" : "Disabled") +
                                    "\nHumidity Alerts: " + (humidityAlertEnabled ? "Enabled" : "Disabled"),
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showSecuritySettingsDialog() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(ProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        final CheckBox twoFactorCheckbox = new CheckBox(this);
        twoFactorCheckbox.setText("Enable 2-Factor Authentication");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(twoFactorCheckbox);

        // Retrieve current 2FA setting
        database.child("users").child(userId).child("twoFactorEnabled").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isEnabled = dataSnapshot.getValue(Boolean.class);
                    twoFactorCheckbox.setChecked(isEnabled != null && isEnabled);
                } else {
                    twoFactorCheckbox.setChecked(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error retrieving 2FA settings", Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Security Settings")
                .setMessage("Configure your security preferences:")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean twoFactorEnabled = twoFactorCheckbox.isChecked();
                    database.child("users").child(userId).child("twoFactorEnabled").setValue(twoFactorEnabled)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ProfileActivity.this,
                                        "2-Factor Authentication " + (twoFactorEnabled ? "enabled" : "disabled"),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ProfileActivity.this,
                                        "Error updating 2FA settings: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}