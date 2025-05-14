package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {
    TextView profileName, profileEmail, profilePhone;
    TextView titleName, titleUsername;
    Button reportBtn;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        titleName = findViewById(R.id.titleName);
        titleUsername = findViewById(R.id.titleUsername);
        reportBtn = findViewById(R.id.reportBtn);

        showAllUserData();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        reportBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        // 🔔 檢查通知 + 跳轉
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notifRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("notifications").child(currentUid);

        notifRef.orderByChild("seen").equalTo(false)
                .limitToFirst(1) // 只抓第一條未讀通知
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot notifSnap : snapshot.getChildren()) {
                                String matchedReportId = notifSnap.child("matchedReportId").getValue(String.class);

                                // 將通知設為已讀
                                notifSnap.getRef().child("seen").setValue(true);

                                // 顯示提示框
                                new AlertDialog.Builder(ProfileActivity.this)
                                        .setTitle("Possible Match Found")
                                        .setMessage("A report similar to yours has been found. Tap OK to view it.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            // ✅ 跳轉至 ReportDetailActivity
                                            Intent intent = new Intent(ProfileActivity.this, ReportDetailActivity.class);
                                            intent.putExtra("reportId", matchedReportId);
                                            startActivity(intent);
                                        })
                                        .setNegativeButton("Later", null)
                                        .show();

                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationCheck", "Firebase error: " + error.getMessage());
                    }
                });
    }


    public void showAllUserData() {
        Intent intent = getIntent();
        String nameUser = intent.getStringExtra("name");
        String emailUser = intent.getStringExtra("email");
        String phoneUser = intent.getStringExtra("phoneNumber");

        titleName.setText(nameUser);
        profileName.setText(nameUser);
        profileEmail.setText(emailUser);
        profilePhone.setText(phoneUser);
    }

    public void passUserData() {
        String userUsername = profilePhone.getText().toString().trim();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nameFromDB = snapshot.child(userUsername).child("name").getValue(String.class);
                    String emailFromDB = snapshot.child(userUsername).child("email").getValue(String.class);
                    String phoneFromDB = snapshot.child(userUsername).child("phoneNumber").getValue(String.class);

                    Intent intent = new Intent(ProfileActivity.this, ReportActivity.class);

                    intent.putExtra("name", nameFromDB);
                    intent.putExtra("email", emailFromDB);
                    intent.putExtra("phone", phoneFromDB);

                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}