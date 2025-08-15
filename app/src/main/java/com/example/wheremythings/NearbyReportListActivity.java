package com.example.wheremythings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class NearbyReportListActivity extends AppCompatActivity {

    private RecyclerView reportRecyclerView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList = new ArrayList<>();
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    private static final double RADIUS_KM = 3.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_report_list);
        reportRecyclerView = findViewById(R.id.reportRecyclerView);
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(this, reportList);
        reportRecyclerView.setAdapter(reportAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance("https://wheremything-47fa4-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("user_reports");

        loadNearbyReports();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadNearbyReports() {
        if (UserLocationHelper.latitude == 0.0 && UserLocationHelper.longitude == 0.0) {
            Toast.makeText(this, "No location available. Please enable location before using this feature.", Toast.LENGTH_LONG).show();
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();

                Location userLoc = new Location("");
                userLoc.setLatitude(UserLocationHelper.latitude);
                userLoc.setLongitude(UserLocationHelper.longitude);

                for (DataSnapshot reportSnap : snapshot.getChildren()) {
                    Report report = reportSnap.getValue(Report.class);
                    if (report != null && report.getLocation() != null) {

                        if (report.getUid().equals(currentUser.getUid())) continue;

                        double reportLat = reportSnap.child("latitude").getValue(Double.class) != null ?
                                reportSnap.child("latitude").getValue(Double.class) : 0.0;
                        double reportLng = reportSnap.child("longitude").getValue(Double.class) != null ?
                                reportSnap.child("longitude").getValue(Double.class) : 0.0;

                        Location reportLoc = new Location("");
                        reportLoc.setLatitude(reportLat);
                        reportLoc.setLongitude(reportLng);

                        float distance = userLoc.distanceTo(reportLoc) / 1000;

                        if (distance <= RADIUS_KM) {
                            report.setId(reportSnap.getKey());
                            reportList.add(report);
                        }
                    }
                }

                reportAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NearbyReportListActivity.this, "Failed to load nearby reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
