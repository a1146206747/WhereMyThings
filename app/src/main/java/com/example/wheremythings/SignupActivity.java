package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {
    EditText signupName, signupPhoneNumber, signupEmail, signupPassword;
    TextView loginRedirectText;
    Button signupButton;
    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPhoneNumber = findViewById(R.id.signup_phoneNumber);
        signupPassword = findViewById(R.id.signup_password);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        signupButton = findViewById(R.id.signup_button);

        mAuth = FirebaseAuth.getInstance();

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = signupName.getText().toString().trim();
                String email = signupEmail.getText().toString().trim();
                String phoneNumber = signupPhoneNumber.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                if (name.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d("SignupActivity", "User registered in Authentication successfully");

                                    // Get the authenticated user
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        String userId = user.getUid();
                                        database = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                        reference = database.getReference("users");

                                        HelperClass helperClass = new HelperClass(name, email, phoneNumber);
                                        reference.child(userId).setValue(helperClass)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d("SignupActivity", "User data saved to Realtime Database successfully");
                                                        Toast.makeText(SignupActivity.this, "You have signed up successfully!", Toast.LENGTH_SHORT).show();

                                                        // Delay jump to LoginActivity to ensure Toast is visible
                                                        new android.os.Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        }, 1000); // Delay 1 second
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("SignupActivity", "Failed to save user data: " + e.getMessage());
                                                        Toast.makeText(SignupActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Log.e("SignupActivity", "Current user is null after registration");
                                        Toast.makeText(SignupActivity.this, "User authentication failed", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("SignupActivity", "Signup failed: " + task.getException().getMessage());
                                    Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}