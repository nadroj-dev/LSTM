package com.otis.lstm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView registerField;
    private Spinner spinnerUserType;
    private String selectedUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        registerField = findViewById(R.id.registerField);
        spinnerUserType = findViewById(R.id.spinnerUserType);

        String[] userTypes = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                userTypes
        );
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerUserType.setAdapter(adapter);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUserType = userTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedUserType = "User";
            }
        });

        signInUser();

        registerField.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void signInUser() {
        findViewById(R.id.buttonSignIn).setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                db.collection("Users").document(user.getUid())
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                DocumentSnapshot document = task1.getResult();
                                                if (document.exists()) {
                                                    Boolean isAdmin = document.getBoolean("admin");
                                                    Intent intent;
                                                    if (isAdmin != null && isAdmin) {
                                                        intent = new Intent(LoginActivity.this, AdminViewActivity.class);
                                                    } else {
                                                        intent = new Intent(LoginActivity.this, MainActivity.class);
                                                    }
                                                    intent.putExtra("USER_TYPE", selectedUserType);
                                                    Toast.makeText(LoginActivity.this,
                                                            "Sign in successful as " + selectedUserType,
                                                            Toast.LENGTH_SHORT).show();
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Toast.makeText(LoginActivity.this,
                                                            "User data not found",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(LoginActivity.this,
                                                        "Error checking user data: " + task1.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Sign in failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}