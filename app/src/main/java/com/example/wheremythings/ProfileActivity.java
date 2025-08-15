package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    Button reportBtn, btnPossibleMatches, nearbyBtn;
    private ImageButton backButton;
    private FusedLocationProviderClient fusedLocationClient;


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
        btnPossibleMatches = findViewById(R.id.btn_possible_matches);

        showAllUserData();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        nearbyBtn = findViewById(R.id.btn_nearby_reports);
        nearbyBtn.setOnClickListener(v -> {
            getCurrentLocationAndOpenNearbyPage();
        });


        btnPossibleMatches.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, PossibleMatchListActivity.class);
            startActivity(intent);
        });

        reportBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notifRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("notifications").child(currentUid);

        notifRef.orderByChild("seen").equalTo(false)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot notifSnap : snapshot.getChildren()) {
                                String matchedReportId = notifSnap.child("matchedReportId").getValue(String.class);

                                notifSnap.getRef().child("seen").setValue(true);

                                new AlertDialog.Builder(ProfileActivity.this)
                                        .setTitle("Possible Match Found")
                                        .setMessage("A report similar to yours has been found. Tap OK to view it.")
                                        .setPositiveButton("OK", (dialog, which) -> {

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
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phoneNumber").getValue(String.class);

                    titleName.setText(name);
                    profileName.setText(name);
                    profileEmail.setText(email);
                    profilePhone.setText(phone);
                } else {
                    Log.e("Profile", "User data not found in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Profile", "Database error: " + error.getMessage());
            }
        });
    }
    private void getCurrentLocationAndOpenNearbyPage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        UserLocationHelper.latitude = location.getLatitude();
                        UserLocationHelper.longitude = location.getLongitude();
                        Log.d("Profile", "📍 Got location: " + UserLocationHelper.latitude + ", " + UserLocationHelper.longitude);
                        
                        startActivity(new Intent(ProfileActivity.this, NearbyReportListActivity.class));
                    } else {
                        Log.w("Profile", "⚠️ Location is null, using fallback 0.0");
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}